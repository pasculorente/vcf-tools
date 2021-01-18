package org.uichuimi.vcf.utils.annotation;

import java.util.List;

public class AnnotationConstants {
	/**
	 * ID for Sift prediction INFO field.
	 */
	public static final String SIFT = "Sift";
	/**
	 * ID for Polyphen prediction INFO field.
	 */
	public static final String POLYPHEN = "Polyphen";
	/**
	 * ID for amino-acid sequence change INFO field.
	 */
	public static final String AMINO = "AMINO";
	/**
	 * ID for gene id INFO field.
	 */
	public static final String ENSG = "ENSG";
	/**
	 * ID for gene symbol INFO field.
	 */
	public static final String SYMBOL = "SYMBOL";
	/**
	 * ID for transcript id INFO field.
	 */
	public static final String ENST = "ENST";
	/**
	 * ID for feature type INFO field.
	 */
	public static final String FT = "FT";
	/**
	 * ID for consequence INFO field.
	 */
	public static final String CONS = "CONS";
	/**
	 * ID for biotype INFO field.
	 */
	public static final String BIO = "BIO";
	/**
	 * ID for hgvs change
	 */
	public static final String HGVS = "HGVS";
	/**
	 * ID for genotype FORMAT column.
	 */
	public static final String GT = "GT";
	/**
	 * ID for read depth INFO and/or FORMAT column.
	 */
	public static final String DP = "DP";
	/**
	 * ID for allele read depth INFO column.
	 */
	public static final String AD = "AD";
	/**
	 * ID for allele count INFO column.
	 */
	public static final String AC = "AC";
	/**
	 * ID for allele frequency INFO column.
	 */
	public static final String AF = "AF";
	/**
	 * ID for allele number INFO column.
	 */
	public static final String AN = "AN";
	/**
	 * Secondary separator to join values inside INFO fields.
	 */
	public static final String DELIMITER = "|";
	/**
	 * Escaped secondary separator to join values inside INFO fields. Use this version when the
	 * method requires a regular expression.
	 *
	 * @see String#split(String)
	 */
	public static final String ESCAPED_DELIMITER = "\\|";
	/**
	 *
	 */
	public static String dbNSFP_SIFT = "dbNSFP_SIFT_score";
	public static String dbNSFP_POLYPHEN = "dbNSFP_Polyphen2_HVAR_score";
	public static String dbNSFP_HGVSp = "dbNSFP_HGVSp_VEP";
	/**
	 * Order of consequences by severity (most severe to least severe)
	 */
	public static final List<String> CONS_SEVERITY = List.of(
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

	private AnnotationConstants() {
	}
}
