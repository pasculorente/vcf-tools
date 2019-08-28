package org.uichuimi.vcf.utils.annotation;

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

	private AnnotationConstants() {
	}
}
