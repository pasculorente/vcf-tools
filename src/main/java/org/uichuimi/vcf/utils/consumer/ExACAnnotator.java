package org.uichuimi.vcf.utils.consumer;

import org.uichuimi.vcf.header.InfoHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.input.VariantContextReader;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Info;
import org.uichuimi.vcf.variant.VariantContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExACAnnotator implements VariantConsumer {

	private final VariantContextReader reader;
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
	 * @param file a single vcf file.
	 */
	public ExACAnnotator(File file) throws IOException {
		this.reader = new VariantContextReader(FileUtils.getInputStream(file));
	}


	@Override
	public void start(VcfHeader header) {
		injectHeaderLines(header);

	}

	private void injectHeaderLines(VcfHeader header) {
		int pos = getLastInfoIndex(header);
		final Collection<InfoHeaderLine> headerLines = new ArrayList<>();
		for (int i = 0; i < IDS.size(); i++)
			headerLines.add(new InfoHeaderLine(IDS.get(i), "A", "Float", DESCRIPTIONS.get(i)));
		header.getHeaderLines().addAll(pos, headerLines);
	}

	private static int getLastInfoIndex(VcfHeader header) {
		int pos = -1;
		for (int i = 0; i < header.getHeaderLines().size(); i++)
			if (header.getHeaderLines().get(i) instanceof InfoHeaderLine)
				pos = i;
		return pos == -1 ? header.getHeaderLines().size() - 1 : pos;
	}


	@Override
	public void accept(VariantContext variant, Coordinate grch38) {
		final VariantContext next = reader.next(grch38);
		if (next != null) annotate(next, variant);
	}

	private void annotate(VariantContext exac, VariantContext variant) {
		final List<String> alleles = exac.getAlternatives();
		final int total = exac.getInfo().getGlobal().getNumber("AN_Adj").intValue();
		for (int a = 0; a < alleles.size(); a++) {
			String allele = alleles.get(a);
			if (variant.getAlternatives().contains(allele)) {
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
				final Info source = exac.getInfo().getAlternativeAllele(a);
				final Info target = variant.getInfo().getAllele(variant.indexOfAllele(allele));
				for (String pop : List.of("AFR", "AMR", "EAS", "FIN", "NFE", "OTH", "SAS")) {
					final int ac = source.getNumber("AC_" + pop).intValue();
					final float freq = (float) ac / total;
					target.set("EX_" + pop + "_AF", freq);
				}
			}
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
