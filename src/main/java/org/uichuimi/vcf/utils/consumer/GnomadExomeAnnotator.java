package org.uichuimi.vcf.utils.consumer;

import java.io.File;
import java.util.List;

public class GnomadExomeAnnotator extends FrequencyAnnotator {

	private final static String fileName = "gnomad.exomes.chr%s.vcf.gz";
	private static final String DATABASE_NAME = "gnomAD exomes";
	private static final String PREFIX = "GE";
	public static final List<String> POPULATIONS = List.of("AFR", "AMR", "ASJ", "EAS", "FIN", "NFE", "OTH", "SAS");
	private static final List<String> KEYS = List.of("AF_afr", "AF_amr", "AF_asj", "AF_eas", "AF_fin", "AF_nfe", "AF_oth", "AF_sas");

	/**
	 * @param gnomad
	 * 		a single vcf file, or a directory with one vcf per chromosome.
	 */
	public GnomadExomeAnnotator(File gnomad) {
		super(gnomad);
	}

	@Override
	String getPrefix() {
		return PREFIX;
	}

	@Override
	String getDatabaseName() {
		return DATABASE_NAME;
	}

	@Override
	List<String> getPopulations() {
		return POPULATIONS;
	}

	@Override
	List<String> getKeys() {
		return KEYS;
	}

	@Override
	protected String getFileName(String chrom) {
		return String.format(fileName, chrom);
	}
}
