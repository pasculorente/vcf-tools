package org.uichuimi.vcf.utils.annotation.consumer;

import org.uichuimi.vcf.variant.Chromosome;

import java.io.File;
import java.util.List;

public class KGenomesAnnotator extends FrequencyAnnotator {

	private static final String KEY = "KG_AF";
	private static final String DATABASE_NAME = "1000 genomes phase 3";
	public static final List<String> POPULATIONS = List.of("AFR", "AMR", "EAS", "EUR", "SAS");
	private static final List<String> KEYS = List.of("AFR_AF", "AMR_AF", "EAS_AF", "EUR_AF", "SAS_AF");

	public KGenomesAnnotator(File file) {
		super(file);
	}

	@Override
	String getKey() {
		return KEY;
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
	protected String getFileName(Chromosome chrom) {
		return null;
	}
}
