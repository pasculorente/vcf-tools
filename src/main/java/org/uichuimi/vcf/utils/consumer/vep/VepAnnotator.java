package org.uichuimi.vcf.utils.consumer.vep;

import org.jetbrains.annotations.NotNull;
import org.uichuimi.vcf.header.InfoHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.utils.consumer.VariantConsumer;
import org.uichuimi.vcf.utils.gff.Gene;
import org.uichuimi.vcf.utils.gff.GeneMap;
import org.uichuimi.vcf.utils.gff.Transcript;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Takes VEP files as source and annotates variants using VEP data. Variants should be provided in
 * coordinate order.
 */
public class VepAnnotator implements VariantConsumer {

	/**
	 * Order of consequences by severity (min to max)
	 */
	private static final List<String> CONS_SEVERITY = List.of(
			"transcript_ablation", "splice_acceptor_variant", "splice_donor_variant", "stop_gained",
			"frameshift_variant", "stop_lost", "start_lost", "transcript_amplification",
			"inframe_insertion", "inframe_deletion", "missense_variant", "protein_altering_variant",
			"splice_region_variant", "incomplete_terminal_codon_variant", "start_retained_variant",
			"stop_retained_variant", "synonymous_variant", "coding_sequence_variant",
			"mature_miRNA_variant", "5_prime_UTR_variant", "3_prime_UTR_variant",
			"non_coding_transcript_exon_variant", "intron_variant", "NMD_transcript_variant",
			"non_coding_transcript_variant", "upstream_gene_variant", "downstream_gene_variant",
			"TFBS_ablation", "TFBS_amplification", "TF_binding_site_variant",
			"regulatory_region_ablation", "regulatory_region_amplification", "feature_elongation",
			"regulatory_region_variant", "feature_truncation", "intergenic_variant"
	);
	private static final String ENSG = "ENSG";
	private static final String ENST = "ENST";
	private static final String AMINO = "AMINO";
	private static final String FT = "FT";
	private static final String SYMBOL = "SYMBOL";
	private static final String BIO = "BIO";
	private static final String CONS = "CONS";
	private static final String POLYPHEN = "Polyphen";
	private static final String SIFT = "Sift";
	private static final String NUMBER_ALLELE = "A";
	private static final String TYPE_STRING = "String";

	private final GeneMap geneMap;
	private final VepReader vepReader;

	public VepAnnotator(File vep, GeneMap geneMap) {
		this.geneMap = geneMap;
		vepReader = new VepReader(vep);
	}

	@Override
	public void start(VcfHeader header) {
		addAnnotationHeaders(header);
	}

	@Override
	public void accept(Variant variant, Coordinate grch38) {
		final Collection<Variant> vepAnnotations = vepReader.getAnnotationList(grch38);
		for (Variant vepAnnotation : vepAnnotations) annotateVep(variant, vepAnnotation, geneMap);
	}

	private void addAnnotationHeaders(VcfHeader header) {
		header.addHeaderLine(new InfoHeaderLine(SIFT, NUMBER_ALLELE, TYPE_STRING, "Sift prediction"), true);
		header.addHeaderLine(new InfoHeaderLine(POLYPHEN, NUMBER_ALLELE, TYPE_STRING, "Polyphen prediction"), true);
		header.addHeaderLine(new InfoHeaderLine(CONS, NUMBER_ALLELE, TYPE_STRING, "Ensembl VEP consequence"), true);
		header.addHeaderLine(new InfoHeaderLine(BIO, NUMBER_ALLELE, TYPE_STRING, "Gene biotype"), true);
		header.addHeaderLine(new InfoHeaderLine(SYMBOL, NUMBER_ALLELE, TYPE_STRING, "Gene symbol"), true);
		header.addHeaderLine(new InfoHeaderLine(FT, NUMBER_ALLELE, TYPE_STRING, "Feature type"), true);
		header.addHeaderLine(new InfoHeaderLine(ENSG, NUMBER_ALLELE, TYPE_STRING, "Ensembl gene id"), true);
		header.addHeaderLine(new InfoHeaderLine(ENST, NUMBER_ALLELE, TYPE_STRING, "Ensembl transcript id"), true);
		header.addHeaderLine(new InfoHeaderLine(AMINO, NUMBER_ALLELE, TYPE_STRING, "Amino acid change (ref/alt)"), true);
	}

	private void annotateVep(Variant variant, Variant vepAnnotation, GeneMap geneMap) {
		// vepAnnotation has alleles, variant has alleles, we need to find those in both to collect the annotations
		final List<String> alleles = vepAnnotation.getAlternatives().stream()
				.filter(variant.getAlternatives()::contains)
				.collect(Collectors.toList());
		if (alleles.isEmpty()) return;

		// ID
		for (String id : vepAnnotation.getIdentifiers())
			if (!variant.getIdentifiers().contains(id))
				variant.getIdentifiers().add(id);

		// VE and RefPep/VarPep can be found inside CSQ
		parseVariantEffect(variant, vepAnnotation);
		parseSift(variant, vepAnnotation);
		parsePolyphen(variant, vepAnnotation);
		parseAmino(variant, vepAnnotation);
	}

	private void parseAmino(Variant variant, Variant annotation) {
		if (!annotation.getInfo().contains("RefPep")
				|| !annotation.getInfo().contains("VarPep")) return;
		final String reference = getReferencePeptide(annotation);
		// VarPep is indexed by allele
		final Map<String, List<String[]>> alternativePeptides = collectAlternativePeptides(annotation);
		List<String> alternatives = variant.getAlternatives();
		final String[] aminos = new String[variant.getAlternatives().size()];
		for (int i = 0; i < alternatives.size(); i++) {
			String allele = alternatives.get(i);
			final List<String[]> list = alternativePeptides.get(allele);
			if (list != null) {
				final String alt = list.get(0)[1];
				aminos[i] = String.format("%s/%s", reference, alt);
			}
		}
		variant.setInfo(AMINO, Arrays.asList(aminos));
	}

	private String getReferencePeptide(Variant annotation) {
		// RefPep is always a single String
		return annotation.getInfo().<List<String>>get("RefPep").get(0);
	}

	/**
	 * Indexes the VarPep array by allele. It interprets the the first value of each element as an
	 * integer, which should be the allele index, then extracts the allele, and adds the element to
	 * the list in the index corresponding to the allele. I.e, transforms this
	 * <pre>
	 * VarPep=
	 *     0|G|LRG_780t2,
	 *     0|G|ENST00000374627,
	 *     1|G|ENST00000374627,
	 *     0|G|ENST00000374630,
	 *     1|G|ENST00000374630,
	 *     0|G|ENST00000400191,
	 *     1|G|ENST00000400191,
	 *     0|G|LRG_780t1,
	 *     0|G|ENST00000374632,
	 *     1|G|ENST00000374632
	 * </pre>
	 * into this
	 * <pre>
	 * A : [
	 *     0|G|LRG_780t2,
	 *     0|G|ENST00000374627,
	 *     0|G|ENST00000374630,
	 *     0|G|ENST00000400191,
	 *     0|G|LRG_780t1,
	 *     0|G|ENST00000374632],
	 * C : [
	 *     1|G|ENST00000374627,
	 *     1|G|ENST00000374630,
	 *     1|G|ENST00000400191,
	 *     1|G|ENST00000374632]
	 * </pre>
	 * being <em>A</em> and <em>C</em> the alternative alleles.
	 *
	 * @param annotation
	 * 		the variant as read from VEP file
	 * @return a map which contains the elements of VarPep indexed
	 */
	private Map<String, List<String[]>> collectAlternativePeptides(Variant annotation) {
		final List<String> varPeps = annotation.getInfo().get("VarPep");
		final Map<String, List<String[]>> alternativePeptides = new HashMap<>();
		for (String element : varPeps) {
			final String[] values = element.split("\\|");
			// some RefPep miss the index
			if (values.length != 3) continue;
			final int index = Integer.parseInt(values[0]);
			final String allele = annotation.getAlternatives().get(index);
			alternativePeptides.computeIfAbsent(allele, a -> new ArrayList<>()).add(values);
		}
		return alternativePeptides;
	}

	private void parseVariantEffect(Variant variant, Variant vepAnnotation) {
		// VE:
		//  Variant effect of a variant overlapping a sequence feature as computed by the ensembl variant effect pipeline.
		//   Format=Consequence|Index|Feature_type|Feature_id.
		//   Index identifies for which variant sequence the effect is described for.
		// VE=
		// missense_variant|0|mRNA|ENST00000618828,
		// splice_region_variant|0|primary_transcript|ENST00000618828,
		// missense_variant|0|mRNA|ENST00000374866,
		// splice_region_variant|0|primary_transcript|ENST00000374866,
		// missense_variant|0|mRNA|ENST00000649066,
		// splice_region_variant|0|primary_transcript|ENST00000649066

		final List<String> ve = vepAnnotation.getInfo().get("VE");
		if (ve == null) return;
		// Collect all consequences by allele
		final Map<String, List<String[]>> alleles = new HashMap<>();
		for (String field : ve) {
			if (field.equals("intergenic_variant")) {
				final List<String> list = repeat("intergenic_variant", variant.getAlternatives().size());
				variant.getInfo().set(CONS, list);
				return;
			}
			final String[] value = field.split("\\|");
			final String allele = value[1];
			alleles.computeIfAbsent(allele, a -> new ArrayList<>()).add(value);
		}
		String[] cons = new String[variant.getAlternatives().size()];
		String[] feat = new String[variant.getAlternatives().size()];
		String[] enst = new String[variant.getAlternatives().size()];
		String[] ensg = new String[variant.getAlternatives().size()];
		String[] bio = new String[variant.getAlternatives().size()];
		String[] symbol = new String[variant.getAlternatives().size()];
		alleles.forEach((index, values) -> {
			final int i = Integer.parseInt(index);
			final String allele = vepAnnotation.getAlternatives().get(i);
			final int pos = variant.getAlternatives().indexOf(allele);
			if (pos >= 0) {
				final String[] effect = mostSevereVariantEffect(values);
				// [0] = Consequence
				// [1] = Index
				// [2] = Feature_type
				// [3] = Feature_id
				cons[pos] = effect[0];
				final Transcript transcript = geneMap.getTranscript(effect[3]);
				if (transcript != null) {
					enst[pos] = transcript.getId();
					feat[pos] = transcript.getType();
					bio[pos] = transcript.getBiotype();
					final Gene gene = transcript.getGene();
					ensg[pos] = gene.getId();
					symbol[pos] = gene.getName();
				}
			}
		});
		setIfAnyNotNull(variant, cons, CONS);
		setIfAnyNotNull(variant, feat, FT);
		setIfAnyNotNull(variant, enst, ENST);
		setIfAnyNotNull(variant, ensg, ENSG);
		setIfAnyNotNull(variant, bio, BIO);
		setIfAnyNotNull(variant, symbol, SYMBOL);
	}

	private void setIfAnyNotNull(Variant variant, String[] cons, String tag) {
		if (Arrays.stream(cons).anyMatch(Objects::nonNull))
			variant.setInfo(tag, Arrays.asList(cons));
	}

	@NotNull
	private <T> List<T> repeat(T value, int n) {
		final List<T> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) list.add(value);
		return list;
	}

	private String[] mostSevereVariantEffect(List<String[]> values) {
		return values.stream().min(Comparator
				// Sort by severity
				.comparingInt((String[] x) -> CONS_SEVERITY.indexOf(x[0]))
				// At the same severity, prefer the Ensembl identifier
				.thenComparingInt(x -> x[3].startsWith(ENST) ? -1 : 1))
				.orElse(null);
	}

	private void parseSift(Variant variant, Variant vepAnnotation) {
		// Prediction for effect of missense variant on protein function as computed by Sift.

		// ID=Sift,Number=.,Type=String
		// Format=Index|Sift_qualitative_prediction|Sift_numerical_value|Feature_id.
		// [0] = The index identifies the missense variant.
		// [1] = Qualitative prediction is tolerated or deleterious.
		// [2] = The numerical value is the normalized probability that the amino acid change is tolerated, so scores nearer 0 are more likely to be deleterious.
		// [3] = transcript

		// Sift=0|deleterious_-_low_confidence|0.02|ENST00000379268,0|deleterious_-_low_confidence|0.01|ENST00000328596,0|deleterious_-_low_confidence|0.02|ENST00000379265;AA=A;RefPep=M;VE=start_lost|0|mRNA|ENST00000379268,start_lost|0|mRNA|ENST00000328596,start_lost|0|mRNA|ENST00000379265;CSQ=G|start_lost|mRNA|ENST00000379265|M/T|deleterious_-_low_confidence(0.02),G|start_lost|mRNA|ENST00000379268|M/T|deleterious_-_low_confidence(0.02),G|start_lost|mRNA|ENST00000328596|M/T|deleterious_-_low_confidence(0.01)
		// Sift=1|tolerated|0.12|ENST00000378567,1|tolerated|0.17|ENST00000468310,1|tolerated|0.13|ENST00000503297
		final List<String> siftInfo = vepAnnotation.getInfo().get(SIFT);
		if (siftInfo == null) return;
		final Map<String, List<String[]>> indexes = collectPredictions(siftInfo);

		final String[] sift = new String[variant.getAlternatives().size()];
		indexes.forEach((index, values) -> {
			final int i = Integer.parseInt(index);
			final String allele = vepAnnotation.getAlternatives().get(i);
			final int pos = variant.getAlternatives().indexOf(allele);
			if (pos >= 0) {
				final String[] value = mostSevereSift(values);
				sift[pos] = value[1].replace("_-_low_confidence", "");
			}
		});
		variant.setInfo(SIFT, Arrays.asList(sift));
	}

	private Map<String, List<String[]>> collectPredictions(List<String> siftInfo) {
		// Collect all consequences by index
		final Map<String, List<String[]>> indexes = new HashMap<>();
		for (String field : siftInfo) {
			final String[] value = field.split("\\|");
			if (value.length < 4) continue;
			final String index = value[0];
			indexes.computeIfAbsent(index, a -> new ArrayList<>()).add(value);
		}
		return indexes;
	}

	private String[] mostSevereSift(List<String[]> values) {
		return values.stream().min(Comparator.comparingDouble(x -> Double.parseDouble(x[2]))).orElse(null);
	}

	private void parsePolyphen(Variant variant, Variant vepAnnotation) {
		// Prediction for effect of missense variant on protein function as computed by Polyphen (human only).

		// ID=Polyphen,Number=.,Type=String
		// Format=Index|Polyphen_qualitative_prediction|Polyphen_numerical_value|Feature_id.
		// [0] = The index identifies the missense variant.
		// [1] = Qualitative prediction (one of probably damaging, possibly damaging, benign or unknown).
		// [2] = Numerical value which is the probability that a substitution is damaging, so values nearer 1 are more confidently predicted to be deleterious.
		// [3] = transcript

		// Polyphen=0|benign|0.017|ENST00000379268,0|benign|0.027|ENST00000328596,0|benign|0.017|ENST00000379265
		// Polyphen=1|benign|0.04|ENST00000378567,1|benign|0.081|ENST00000468310,1|benign|0.04|ENST0000050329

		final List<String> polyphen = vepAnnotation.getInfo().get(POLYPHEN);
		if (polyphen == null) return;
		// Collect all predictions by index
		final Map<String, List<String[]>> indexes = collectPredictions(polyphen);

		final String[] ppn = new String[variant.getAlternatives().size()];
		indexes.forEach((index, values) -> {
			final int i = Integer.parseInt(index);
			final String allele = vepAnnotation.getAlternatives().get(i);
			final int pos = variant.getAlternatives().indexOf(allele);
			if (pos >= 0) {
				final String[] value = mostSeverePolyphen(values);
				ppn[pos] = value[1];
			}
		});
		variant.setInfo(POLYPHEN, Arrays.asList(ppn));
	}

	private String[] mostSeverePolyphen(List<String[]> values) {
		return values.stream().max(Comparator.comparingDouble(x -> Double.valueOf(x[2]))).orElse(null);
	}
	@Override
	public void close() {
		try {
			vepReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
