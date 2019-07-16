package org.uichuimi.vcf.utils.consumer;

import org.uichuimi.vcf.header.InfoHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ExACAnnotator implements VariantConsumer {

	private final VariantReader reader;
	private static final List<String> IDS = List.of(
			"EX_AFR_AF", "EX_AMR_AF", "EX_EAS_AF", "EX_FIN_AF",
			"EX_NFE_AF", "EX_OTH_AF", "EX_SAS_AF");
	private static final List<String> DESCRIPTIONS = List.of(
			"Allele frequency in African population (ExAC)",
			"Allele frequency in American population (ExAC)",
			"Allele frequency in East Asian population (ExAC)",
			"Allele frequency in Finnish population (ExAC)",
			"Allele frequency in European Non-Finnish population (ExAC)",
			"Allele frequency in other population (ExAC)",
			"Allele frequency in South Asia population (ExAC)"
	);

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
		for (int i = 0; i < IDS.size(); i++)
			headerLines.add(new InfoHeaderLine(IDS.get(i), "A", "Float", DESCRIPTIONS.get(i)));
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
		for (String pop : List.of("AFR", "AMR", "EAS", "FIN", "NFE", "OTH", "SAS")) {
			final Float[] frs = new Float[variant.getAlternatives().size()];
			for (int variantIndex = 0; variantIndex < variant.getAlternatives().size(); variantIndex++) {
				final int exacIndex = exac.getAlternatives().indexOf(variant.getAlternatives().get(variantIndex));
				if (exacIndex < 0) continue; // Allele not available
				final Integer ac = exac.getInfo().<List<Integer>>get("AC_" + pop).get(exacIndex);
				final float freq = (float) ac / total;
				frs[variantIndex] = freq;
			}
			variant.setInfo("EX_" + pop + "_AF", Arrays.asList(frs));
		}
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
