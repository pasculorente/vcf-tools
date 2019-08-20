package org.uichuimi.vcf.utils.consumer;

import java.io.File;
import java.util.List;

public class GnomadGenomeAnnotator extends FrequencyAnnotator {

	private final static String fileName = "gnomad.genomes.chr%s.vcf.gz";
	private static final List<String> KEYS = List.of("AF_afr", "AF_amr", "AF_eas", "AF_nfe", "AF_fin", "AF_oth", "AF_asj");
	public static final List<String> POPULATIONS = List.of("AFR", "AMR", "EAS", "NFE", "FIN", "OTH", "ASJ");
	private static final String DATABASE_NAME = "genomAD genomes";
	private static final String PREFIX = "GG";

	/**
	 * @param source a single vcf file, or a directory with one vcf per chromosome.
	 */
	public GnomadGenomeAnnotator(File source) {
		super(source);
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
