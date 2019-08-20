package org.uichuimi.vcf.utils.consumer;

import org.uichuimi.vcf.header.InfoHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;
import org.uichuimi.vcf.variant.VcfConstants;
import org.uichuimi.vcf.variant.VcfType;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class ExACAnnotator implements VariantConsumer {
	private static final NumberFormat DECIMAL = new DecimalFormat("#.###");
	private static final List<String> POPULATIONS = List.of("AFR", "AMR", "EAS", "FIN", "NFE", "OTH", "SAS");
	private static final String SECONDARY_SEPARATOR = "|";
	private static final String DESCRIPTION = String.format("Allele frequency from ExAC (%s)", String.join(SECONDARY_SEPARATOR, POPULATIONS));
	private final VariantReader reader;

	/**
	 * @param file
	 * 		a single vcf file.
	 */
	public ExACAnnotator(File file) throws IOException {
		this.reader = new VariantReader(FileUtils.getInputStream(file));
	}

	@Override
	public void start(VcfHeader header) {
		injectHeaderLines(header);
	}

	private void injectHeaderLines(VcfHeader header) {
		final Collection<InfoHeaderLine> headerLines = new ArrayList<>();
		headerLines.add(new InfoHeaderLine("EX_AF", VcfConstants.NUMBER_A, VcfType.STRING, DESCRIPTION));
		for (InfoHeaderLine line : headerLines) header.addHeaderLine(line, true);
	}

	@Override
	public void accept(Variant variant, Coordinate grch38) {
		final Variant next = reader.next(grch38);
		if (next != null) annotate(next, variant);
	}

	private void annotate(Variant exac, Variant variant) {
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
				final float freq = (float) ac / total;
				freqs[a][popIndex] = freq;
			}
		}
		final List<String> ex_af = new ArrayList<>(variant.getAlternatives().size());
		for (double[] freq : freqs) {
			// if any of the frequencies is available, the rest goes to null
			if (Arrays.stream(freq).anyMatch(Double::isFinite)) {
				final StringJoiner joiner = new StringJoiner(SECONDARY_SEPARATOR);
				for (double v : freq) {
					if (Double.isFinite(v)) joiner.add(DECIMAL.format(v));
					else joiner.add(VcfConstants.EMPTY_VALUE);
				}
				ex_af.add(joiner.toString());
			} else {
				ex_af.add(VcfConstants.EMPTY_VALUE);
			}
		}
		if (ex_af.stream().anyMatch(s -> !s.equals(VcfConstants.EMPTY_VALUE)))
			variant.setInfo("EX_AF", ex_af);
	}

	@Override
	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
