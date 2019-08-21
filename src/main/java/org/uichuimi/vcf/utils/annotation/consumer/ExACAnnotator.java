package org.uichuimi.vcf.utils.annotation.consumer;

import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.util.List;

public class ExACAnnotator extends FrequencyAnnotator {

	public static final List<String> POPULATIONS = List.of("AFR", "AMR", "EAS", "FIN", "NFE", "OTH", "SAS");
	private static final String PREFIX = "EX";
	private static final String DATABASE_NAME = "ExAC";

	public ExACAnnotator(File file) {
		super(file);
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
		return null;
	}

	@Override
	protected String getFileName(String chrom) {
		return null;
	}

	@Override
	double[][] createFrequencies(Variant variant, Variant exac) {
		// AC_AFR=0
		// AC_AMR=0
		// AC_EAS=0
		// AC_FIN=0
		// AC_NFE=0
		// AC_OTH=0
		// AC_SAS=2
		//
		// AN_AFR=770
		// AN_AMR=134
		// AN_Adj=8432
		// AN_EAS=254
		// AN_FIN=16
		// AN_NFE=2116
		// AN_OTH=90
		// AN_SAS=5052
		// AN_Adj=8432
		final int total = exac.getInfo("AN_Adj");
		final double[][] freqs = new double[variant.getAlternatives().size()][POPULATIONS.size()];
		for (int popIndex = 0; popIndex < POPULATIONS.size(); popIndex++) {
			String pop = POPULATIONS.get(popIndex);
			for (int a = 0; a < variant.getAlternatives().size(); a++) {
				final int exacIndex = exac.getAlternatives().indexOf(variant.getAlternatives().get(a));
				if (exacIndex < 0) continue; // Allele not available
				final Integer ac = exac.getInfo().<List<Integer>>get("AC_" + pop).get(exacIndex);
				final double freq = (double) ac / total;
				freqs[a][popIndex] = freq;
			}
		}
		return freqs;
	}
}
