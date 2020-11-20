package org.uichuimi.vcf.utils.annotation;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uichuimi.vcf.variant.Chromosome;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class VariantAnnotatorTest {

	private final List<String> tugores = List.of(
			"3_prime_UTR_variant",
			"5_prime_UTR_variant",
			"NMD_transcript_variant",
			"TFBS_amplification",
			"TF_binding_site_variant",
			"coding_sequence_variant",
			"downstream_gene_variant",
			"feature_elongation",
			"feature_truncation",
			"intergenic_variant",
			"intron_variant",
			"mature_miRNA_variant",
			"non_coding_transcript_exon_variant",
			"non_coding_transcript_variant",
			"regulatory_region_amplification",
			"regulatory_region_variant",
			"upstream_gene_variant",
			"incomplete_terminal_codon_variant",
			"splice_region_variant",
			"start_lost",
			"stop_retained_variant",
			"synonymous_variant",
			"TFBS_ablation",
			"missense_variant",
			"protein_altering_variant",
			"regulatory_region_ablation",
			"inframe_deletion",
			"inframe_insertion",
			"frameshift_variant",
			"splice_acceptor_variant",
			"splice_donor_variant",
			"stop_gained",
			"stop_lost",
			"transcript_ablation",
			"transcript_amplification");
	private final List<String> vep = List.of(
			"intergenic_variant",
			"feature_truncation",
			"regulatory_region_variant",
			"feature_elongation",
			"regulatory_region_amplification",
			"regulatory_region_ablation",
			"TF_binding_site_variant",
			"TFBS_amplification",
			"TFBS_ablation",
			"downstream_gene_variant",
			"upstream_gene_variant",
			"non_coding_transcript_variant",
			"NMD_transcript_variant",
			"intron_variant",
			"non_coding_transcript_exon_variant",
			"3_prime_UTR_variant",
			"5_prime_UTR_variant",
			"mature_miRNA_variant",
			"coding_sequence_variant",
			"synonymous_variant",
			"stop_retained_variant",
			"start_retained_variant",
			"incomplete_terminal_codon_variant",
			"splice_region_variant",
			"protein_altering_variant",
			"missense_variant",
			"inframe_deletion",
			"inframe_insertion",
			"transcript_amplification",
			"start_lost",
			"stop_lost",
			"frameshift_variant",
			"stop_gained",
			"splice_donor_variant",
			"splice_acceptor_variant",
			"transcript_ablation");
	private final List<String> snp = List.of(
			"non_coding_transcript_exon_variant",
			"intergenic_variant",
			"intragenic_variant",
			"intron_variant",
			"regulatory_region_variant",
			"TF_binding_site_variant",
			"downstream_gene_variant",
			"upstream_gene_variant",
			"5_prime_UTR_premature_start_codon_gain_variant",
			"3_prime_UTR_variant",
			"5_prime_UTR_variant",
			"coding_sequence_variant",
			"stop_retained_variant",
			"synonymous_variant",
			"initiator_codon_variant",
			"stop_retained_variant",
			"splice_region_variant",
			"splice_branch_variant",
			"conservative_inframe_deletion",
			"disruptive_inframe_deletion",
			"conservative_inframe_insertion",
			"disruptive_inframe_insertion",
			"missense_variant",
			"rare_amino_acid_variant",
			"splice_donor_variant",
			"splice_acceptor_variant",
			"start_lost",
			"stop_lost",
			"stop_gained",
			"frameshift_variant");

	@Test
	void debug() throws IOException {
		final File input = new File(getClass().getResource("/files/input.vcf").getFile());
		final File exac = new File(getClass().getResource("/files/ExAC.vcf").getFile());
		final File dbsnp = new File(getClass().getResource("/files/dbSNP.vcf").getFile());
		final File vep = new File(getClass().getResource("/files/vep.vcf").getFile());
		final File genes = new File(getClass().getResource("/files/Homo_sapiens.GRCh38.95.gff3.gz").getFile());
		final File gnomadGenomes = new File(getClass().getResource("/files/gnomad_genomes.vcf").getFile());
		final File gnomadExomes = new File(getClass().getResource("/files/gnomad_exomes.vcf").getFile());
		final String data = generateOutputData(input, exac, dbsnp, vep, genes, gnomadGenomes, gnomadExomes);
		final String expected = IOUtils.toString(getClass().getResourceAsStream("/files/expected_output.vcf"), Charset.defaultCharset());
		Assertions.assertEquals(expected, data);
	}

	private String generateOutputData(File input, File exac, File dbsnp, File vep, File genes, File gnomadGenomes, File gnomadExomes) {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			final VariantAnnotator annotator = new VariantAnnotator(List.of(input), null)
					.setNamespace(Chromosome.Namespace.UCSC)
					.setCompute(true)
					.setSnpeff(true)
					.setExac(exac)
					.setGenes(genes)
					.setGnomadExomes(gnomadExomes)
					.setGnomadGenomes(gnomadGenomes)
					.setVep(vep)
					.setDbsnp(dbsnp)
					.setOutputStream(os);
			annotator.call();
			return os.toString();
		} catch (Exception e) {
			Assertions.fail(e);
		}
		return "";
	}

	@Test
	void sats() {
		final List<String> sorted = Stream.of(vep, tugores, snp).flatMap(Collection::stream)
				.distinct()
				.sorted(this::compare)
				.collect(Collectors.toList());
		sorted.forEach(System.out::println);
		for (List<String> list : Arrays.asList(vep, tugores, snp)) {
			System.out.println();
			for (String item : list)
				System.out.printf("%s %d%n", item, sorted.indexOf(item));
		}
	}

	private int compare(String a, String b) {
		final List<Integer> comparisons = new ArrayList<>();
		if (tugores.contains(a) && tugores.contains(b))
			comparisons.add(Integer.compare(tugores.indexOf(a), tugores.indexOf(b)));
		if (vep.contains(a) && vep.contains(b))
			comparisons.add(Integer.compare(vep.indexOf(a), vep.indexOf(b)));
		if (snp.contains(a) && snp.contains(b))
			comparisons.add(Integer.compare(snp.indexOf(a), snp.indexOf(b)));
		if (comparisons.isEmpty()) {
			System.err.printf("%s %s not in any list%n", a, b);
			return 0;
		}
		if (comparisons.size() == 1) return comparisons.get(0);
		final Map<Integer, Integer> counts = new TreeMap<>();
		for (Integer comparison : comparisons) counts.merge(comparison, 1, Integer::sum);
		final List<Map.Entry<Integer, Integer>> sortedEntries = counts.entrySet().stream()
				.sorted(Comparator.<Map.Entry<Integer, Integer>>comparingInt(Map.Entry::getValue).reversed())
				.collect(Collectors.toList());
		if (sortedEntries.size() == 1) return sortedEntries.get(0).getKey();
		if (sortedEntries.get(0).getValue() > sortedEntries.get(1).getValue())
			return sortedEntries.get(0).getKey();
		System.err.printf("%s %s (%s)%n", a, b, comparisons);
		return 0;
	}

}
