package org.uichuimi.vcf.utils.neo4j;

import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.utils.Genotype;
import org.uichuimi.vcf.utils.annotate.VariantConsumer;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Info;
import org.uichuimi.vcf.variant.VariantContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Creates 3rd normal form tables that can easily being imported into neo4j.
 * <p>
 * <ul>
 * <li><b>samples -> </b>id:ID(sample)</li>
 * <li><b>variant -> </b>:ID(variant), chrom, pos, ref[], alt[], rs[], sift[], polyphen[], effect[], amino[]</li>
 * <li><b>frequencies -> </b>:ID(freq), source, population, value:double[]</li>
 * <li><b>var2freq -> </b>:START_ID(variant), :END_ID(freq)</li>
 * <li><b>var2gene -> </b>:START_ID(variant), :END_ID(gene)</li>
 * <li><b>homozygous -> </b>:START_ID(sample), :END_ID(variant), ad, dp:int</li>
 * <li><b>heterozygous -> </b>:START_ID(sample), :END_ID(variant), ad, dp:int</li>
 * <li><b>wildtype -> </b>:START_ID(sample), :END_ID(variant), ad, dp:int</li>
 * </ul>
 * <i>gene</i> refers to Ensembl gene identifier (ENSG0001234). This class is not responsible of the genes table.
 */
public class Neo4jTablesWriter implements VariantConsumer {

	private final TableWriter samples;
	private final TableWriter homozygous;
	private final TableWriter heterozygous;
	private final TableWriter wildtype;
	private final TableWriter var2gene;
	private final TableWriter variants;
	private final TableWriter frequencies;
	private final TableWriter var2freq;
	private VcfHeader header;


	private AtomicLong frequencyId = new AtomicLong();

	public Neo4jTablesWriter(File path) throws IOException {

		samples = new TableWriter(new File(path, "Persons.tsv"), Collections.singletonList("id:ID(sample)"));

		// (:Sample)-[:homozygous|heterozygous|wildtype]->(:Variant)
		final List<String> columns = Arrays.asList(":START_ID(sample)", ":END_ID(variant)", "ad:int[]", "dp:int");
		homozygous = new TableWriter(new File(path, "homo.tsv"), columns);
		heterozygous = new TableWriter(new File(path, "hetero.tsv"), columns);
		wildtype = new TableWriter(new File(path, "wild.tsv"), columns);

		// (:Variant)
		final List<String> cols = new ArrayList<>(Arrays.asList(":ID(variant)", "chrom", "pos", "ref:string",
				"alt:string", "rs:string[]", "sift:string", "polyphen:string", "effect:string", "amino:string"));
		variants = new TableWriter(new File(path, "Variants.tsv"), cols);

		// (:Variant)-[:gene]->(:Gene)
		var2gene = new TableWriter(new File(path, "var2gene.tsv"), Arrays.asList(":START_ID(variant)", ":END_ID(gene)"));

		// (:Variant)-[:FREQUENCY]->(:Frequency)
		frequencies = new TableWriter(new File(path, "Frequencies.tsv"), Arrays.asList(":ID(freq)", "source", "population", "value:double"));
		var2freq = new TableWriter(new File(path, "var2freq.tsv"), Arrays.asList(":START_ID(variant)", ":END_ID(freq)"));
	}

	@Override
	public void start(VcfHeader header) {
		this.header = header;
		try {
			for (String sample : header.getSamples()) {
				samples.write(sample);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void accept(VariantContext variant, Coordinate coordinate) {
		try {
			for (int r = 0; r < variant.getReferences().size(); r++)
				for (int a = 0; a < variant.getAlternatives().size(); a++)
					addSimplifiedVariant(variant, coordinate, r, a);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void addSimplifiedVariant(VariantContext variant, Coordinate coordinate, int r, int a) throws IOException {
		final String ref = variant.getReferences().get(r);
		final String alt = variant.getAlternatives().get(a);
		final String variantId = String.format("%s:%s:%s:%s",
				coordinate.getChrom(),
				coordinate.getPosition(),
				ref,
				alt);
		final int absoluteA = variant.getReferences().size() + a;

		final Info info = variant.getInfo().getAlternativeAllele(a);
		// Variant
		variants.write(variantId, coordinate.getChrom(), coordinate.getPosition(),
				ref,
				alt,
				String.join(",", variant.getIds()),
				info.get("Sift"),
				info.get("Polyphen"),
				info.get("CONS"),
				info.get("AMINO")
		);
		// Frequencies
		for (String pop : Arrays.asList("EAS", "SAS", "EUR", "AFR", "AMR")) {
			final Object score = info.get(pop + "_AF");
			if (score != null) {
				final long id = frequencyId.incrementAndGet();
				var2freq.write(variantId, id);
				frequencies.write(id, "1000G", pop, score);
			}
		}
		// Samples
		for (int i = 0; i < header.getSamples().size(); i++) {
			final String sample = header.getSamples().get(i);
			final String gt = variant.getSampleInfo(i).getGlobal().getString("GT");
			if (gt == null || gt.equals("./.") || gt.equals(".")) continue;
			final Genotype genotype = Genotype.create(gt);
			// AD has Number=., so it is in global
			final Object[] ad = variant.getSampleInfo(i).getGlobal().getArray("AD");
			final String aad = ad == null ? "0,0" : String.format("%s,%s", ad[r], ad[absoluteA]);
			// DP has Number=1, we take it from global
			String dp = variant.getSampleInfo(i).getGlobal().getString("DP");
			if (dp == null) dp = "0";
			if (genotype.getA() == r && genotype.getB() == r)
				wildtype.write(sample, variantId, aad, dp);
			else if (genotype.getA() == absoluteA && genotype.getB() == absoluteA)
				homozygous.write(sample, variantId, aad, dp);
			else heterozygous.write(sample, variantId, aad, dp);
		}

		// Genes
		final String ensg = info.getString("ENSG");
		if (ensg != null) var2gene.write(variantId, ensg);
	}

	@Override
	public void close() {

	}
}
