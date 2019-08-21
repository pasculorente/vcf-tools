package org.uichuimi.vcf.utils.consumer.neo4j;

import org.jetbrains.annotations.NotNull;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.utils.Genotype;
import org.uichuimi.vcf.utils.consumer.*;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Info;
import org.uichuimi.vcf.variant.Variant;
import org.uichuimi.vcf.variant.VcfConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Creates 3rd normal form tables that can easily being imported into neo4j.
 * <p>
 * <ul>
 * <li><b>samples -> </b>id:ID(sample)</li>
 * <li><b>variant -> </b>:ID(variant), chrom, pos, ref, alt, rs[], sift, polyphen, effect,
 * amino</li>
 * <li><b>frequencies -> </b>:ID(freq), source, population, value:double</li>
 * <li><b>var2freq -> </b>:START_ID(variant), :END_ID(freq)</li>
 * <li><b>var2gene -> </b>:START_ID(variant), :END_ID(gene)</li>
 * <li><b>homozygous -> </b>:START_ID(sample), :END_ID(variant), ad[], dp:int</li>
 * <li><b>heterozygous -> </b>:START_ID(sample), :END_ID(variant), ad[], dp:int</li>
 * <li><b>wildtype -> </b>:START_ID(sample), :END_ID(variant), ad[], dp:int</li>
 * </ul>
 * <i>gene</i> refers to Ensembl gene identifier (ENSG0001234). This class is not responsible of
 * the genes table.
 */
public class Neo4jTablesWriter implements VariantConsumer {

	private final static AtomicLong NEXT_ID = new AtomicLong();
	private final TableWriter samples;
	private final TableWriter homozygous;
	private final TableWriter heterozygous;
	private final TableWriter wildtype;
	private final TableWriter var2gene;
	private final TableWriter variants;
	private final TableWriter frequencies;
	private final TableWriter var2freq;
	private final TableWriter var2effect;
	private VcfHeader header;


	private final AtomicLong frequencyId = new AtomicLong();

	public Neo4jTablesWriter(File path) throws IOException {

		samples = new TableWriter(new File(path, "Persons.tsv.gz"), Collections.singletonList("identifier:ID(sample)"));
		samples.createIndex(0);

		// (:Sample)-[:homozygous|heterozygous|wildtype]->(:Variant)
		final List<String> columns = List.of(":START_ID(sample)", ":END_ID(variant)", "ad:int[]", "dp:int");
		homozygous = new TableWriter(new File(path, "homo.tsv.gz"), columns);
		heterozygous = new TableWriter(new File(path, "hetero.tsv.gz"), columns);
		wildtype = new TableWriter(new File(path, "wild.tsv.gz"), columns);

		// (:Variant)
		final List<String> cols = new ArrayList<>(List.of(":ID(variant)", "chrom", "pos:int",
				"ref:string", "alt:string", "identifier:string", "sift:string", "polyphen:string",
				"amino:string"));
		variants = new TableWriter(new File(path, "Variants.tsv.gz"), cols);

		// (:Variant)-[:PREDICTION]->(:Prediction{source:"sift", prediction: "benign"})
//		predictions = new TableWriter(new File(path, "Predictions.tsv.gz"), List.of(":ID(prediction)", "source", "prediction"));
//		var2prediction = new TableWriter(new File(path, "var2prediction.tsv.gz"), List.of(":START_ID(variant)", ":END_ID(prediction)"));

		var2effect = new TableWriter(new File(path, "var2effect.tsv.gz"), List.of(":START_ID(variant)", ":END_ID(effect)"));

		// (:Variant)-[:gene]->(:Gene)
		var2gene = new TableWriter(new File(path, "var2gene.tsv.gz"), List.of(":START_ID(variant)", ":END_ID(gene)"));

		// (:Variant)-[:FREQUENCY]->(:Frequency)
		frequencies = new TableWriter(new File(path, "Frequencies.tsv.gz"), List.of(":ID(freq)", "source", "population", "value:double"));
		var2freq = new TableWriter(new File(path, "var2freq.tsv.gz"), List.of(":START_ID(variant)", ":END_ID(freq)"));
	}

	@Override
	public void start(VcfHeader header) {
		this.header = header;
		writeSamples(header);
	}

	private void writeSamples(VcfHeader header) {
		try {
			for (String sample : header.getSamples()) samples.write(sample);
			samples.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void accept(Variant variant, Coordinate grch38) {
		try {
			for (int r = 0; r < variant.getReferences().size(); r++)
				for (int a = 0; a < variant.getAlternatives().size(); a++)
					addSimplifiedVariant(variant, grch38, r, a);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adds a variant to the variants table. Since third normal form requires only 1 reference
	 * allele and 1 alternative allele, this method should be called <em>r * a</em> times, to export
	 * all combinations in the same variant.
	 *
	 * @param variant
	 * 		the variant
	 * @param coordinate
	 * 		the target coordinate
	 * @param r
	 * 		index of reference allele in variant.references
	 * @param a
	 * 		index of alternative allele in variant.alternatives
	 * @throws IOException
	 * 		if any writer is closed
	 */
	private void addSimplifiedVariant(Variant variant, Coordinate coordinate, int r, int a) throws IOException {
		final int absoluteA = variant.getReferences().size() + a;
		final String variantId = writeVariant(variant, coordinate, a, r);

		writeFrequencies(variant, variantId, a, "1000G", "KG_AF", KGenomesAnnotator.POPULATIONS);
		writeFrequencies(variant, variantId, a, "gnomAD_genomes", "GG_AF", GnomadGenomeAnnotator.POPULATIONS);
		writeFrequencies(variant, variantId, a, "gnomAD_exomes", "GE_AF", GnomadExomeAnnotator.POPULATIONS);
		writeFrequencies(variant, variantId, a, "ExAC", "EX_AF", ExACAnnotator.POPULATIONS);

		writeConsequence(variant, variantId, a);
		writeGene(variant, variantId, a);
		writeSamples(variant, variantId, r, absoluteA);
	}

	private String writeVariant(Variant variant, Coordinate coordinate, int a, int r) throws IOException {
		final String ref = variant.getReferences().get(r);
		final String alt = variant.getAlternatives().get(a);
		final String chrom = coordinate.getChrom();
		final int position = coordinate.getPosition();

		final String variantId = String.format("%s:%s:%s:%s", chrom, position, ref, alt);
		final List<String> sift = variant.getInfo("Sift");
		final List<String> phen = variant.getInfo("Polyphen");
		final List<String> amino = variant.getInfo("AMINO");
		final String identifier = variant.getIdentifiers().isEmpty()
				? "n" + NEXT_ID.incrementAndGet()
				: variant.getIdentifiers().get(0);
		variants.write(variantId, chrom, position, ref, alt,
				identifier,
				sift == null ? null : sift.get(a),
				phen == null ? null : phen.get(a),
				amino == null ? null : amino.get(a)
		);
		return variantId;
	}

	private void writeGene(Variant variant, String variantId, int a) throws IOException {
		final List<String> genes = variant.getInfo("ENSG");
		if (genes == null) return;
		final String ensg = genes.get(a);
		if (ensg != null) var2gene.write(variantId, ensg);
	}

	private void writeConsequence(Variant variant, String variantId, int a) throws IOException {
		// CONS is expected to be Number=A or Number=.
		final List<String> cons = variant.getInfo("CONS");
		if (cons == null) return;
		final String effect = cons.get(a);
		if (effect != null) var2effect.write(variantId, effect);
	}

	private void writeSamples(Variant variant, String variantId, int r, int absoluteA) throws IOException {
		for (int i = 0; i < header.getSamples().size(); i++) {
			final String sample = header.getSamples().get(i);
			final Info sampleInfo = variant.getSampleInfo(i);
			final String gt = sampleInfo.get("GT");
			if (gt == null || gt.equals("./.") || gt.equals(".")) continue;

			final String alleleDepth = getAlleleDepth(r, absoluteA, sampleInfo);
			final int readDepth = getReadDepth(sampleInfo);

			final Genotype genotype = Genotype.create(gt);
			if (genotype.getA() == r && genotype.getB() == r)
				wildtype.write(sample, variantId, alleleDepth, readDepth);
			else if (genotype.getA() == absoluteA && genotype.getB() == absoluteA)
				homozygous.write(sample, variantId, alleleDepth, readDepth);
			else if (genotype.getA() == absoluteA || genotype.getB() == absoluteA)
				heterozygous.write(sample, variantId, alleleDepth, readDepth);
		}
	}

	private void writeFrequencies(Variant variant, String variantId, int a, String database, String key, List<String> populations) throws IOException {
		// Frequencies are Number=A, so a must be position in alternatives
		final List<String> freqs = variant.getInfo(key);
		if (freqs == null) return;
		if (freqs.size() <= a) return;
		final String fr = freqs.get(a);
		if (fr.equals(VcfConstants.EMPTY_VALUE)) return;
		final String[] values = fr.split("\\|");
		for (int p = 0; p < populations.size(); p++) {
			final String value = values[p];
			if (value.equals(VcfConstants.EMPTY_VALUE)) continue;
			final long id = frequencyId.incrementAndGet();
			var2freq.write(variantId, id);
			frequencies.write(id, database, populations.get(p), value);
		}
	}

	@NotNull
	private Integer getReadDepth(Info sampleInfo) {
		final Integer dp = sampleInfo.get("DP");
		return dp == null ? 0 : dp;
	}

	private String getAlleleDepth(int r, int absoluteA, Info sampleInfo) {
		final int refAd;
		final int altAd;
		final List<Integer> ad = sampleInfo.get("AD");
		if (ad == null) {
			refAd = 0;
			altAd = 0;
		} else {
			refAd = ad.get(r);
			altAd = ad.get(absoluteA);
		}
		return String.format("%d,%d", refAd, altAd);
	}

	@Override
	public void close() {

	}
}
