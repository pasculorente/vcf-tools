package org.uichuimi.vcf.utils.annotation.consumer;

import org.jetbrains.annotations.NonNls;
import org.uichuimi.vcf.variant.Chromosome;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ExACAnnotator extends FrequencyAnnotator {

	public static final List<String> POPULATIONS = List.of("AFR", "AMR", "EAS", "FIN", "NFE", "OTH", "SAS");

	private static final List<String> PREFIXED = POPULATIONS.stream().map(pop -> "AC_" + pop).collect(Collectors.toList());

	private static final String KEY = "EX_AF";
	private static final String DATABASE_NAME = "ExAC";
	@NonNls
	private static final String AN_ADJ = "AN_Adj";

	public ExACAnnotator(File file) {
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
		return null;
	}

	@Override
	protected String getFileName(Chromosome chrom) {
		return null;
	}

	@Override
	double[][] createFrequencies(Variant variant, Collection<Variant> annotations) {
		// AC_AFR=0
		// AC_AMR=0
		// AC_EAS=0
		// AC_FIN=0
		// AC_NFE=0
		// AC_OTH=0
		// AC_SAS=2

		// AN_AFR=770
		// AN_AMR=134
		// AN_Adj=8432
		// AN_EAS=254
		// AN_FIN=16
		// AN_NFE=2116
		// AN_OTH=90
		// AN_SAS=5052
		// AN_Adj=8432
		final double[][] freqs = new double[variant.getAlternatives().size()][POPULATIONS.size()];
		for (double[] freq : freqs) Arrays.fill(freq, -1);
		for (Variant exac : annotations) {
			final int total = exac.getInfo(AN_ADJ);
			List<String> alternatives = exac.getAlternatives();
			for (int a = 0; a < alternatives.size(); a++) {
				final String alternative = alternatives.get(a);
				final int va = variant.getAlternatives().indexOf(alternative);
				if (va < 0) continue;
				for (int p = 0; p < PREFIXED.size(); p++) {
					final Integer ac = exac.<List<Integer>>getInfo(PREFIXED.get(p)).get(a);
					final double freq = (double) ac / total;
					freqs[va][p] = freq;
				}
			}
		}
		return freqs;
	}
}
