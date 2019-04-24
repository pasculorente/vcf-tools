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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GnomadAnnotator implements VariantConsumer {

	private final static String fileName = "gnomad.%s.vcf.gz";

	private final static List<String> GNOMAD_INFO = List.of("AF_afr", "AF_amr", "AF_eas", "AF_nfe", "AF_fin", "AF_asj", "AF_oth");
	private final static List<String> VARIANT_INFO = List.of("G_AFR_AF", "G_AMR_AF", "G_EAS_AF", "G_NFE_AF", "G_FIN_AF", "G_ASJ_AF", "G_OTH_AF");

	private final File gnomad;
	private VcfHeader header;

	private VariantContextReader reader;
	private String openChromosome;


	/**
	 * @param gnomad a single vcf file, or a directory with one vcf per chromosome.
	 * @throws IOException if gnomad is not readable
	 */
	public GnomadAnnotator(File gnomad) throws IOException {
		this.gnomad = gnomad;
		if (gnomad.isFile()) reader = new VariantContextReader(FileUtils.getInputStream(gnomad));
	}

	@Override
	public void start(VcfHeader header) {
		this.header = header;
		addInfoHeaders();
	}

	private void addInfoHeaders() {
		int start = -1;
		int end = 1;
		for (int i = 0; i < header.getHeaderLines().size(); i++) {
			if (header.getHeaderLines().get(i) instanceof InfoHeaderLine) {
				if (start == -1) {
					start = i;
					end = i;
				} else end = i;
			}
		}
		header.getHeaderLines().add(end, new InfoHeaderLine(Map.of(
				"ID", "G_AFR_AF",
				"Number", "A",
				"Type", "Float",
				"Description", "Allele frequency in African population (gnomAD)")));
		header.getHeaderLines().add(end, new InfoHeaderLine(new LinkedHashMap<>() {{
			put("ID", "G_AMR_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in American population (gnomAD)");
		}}));
		header.getHeaderLines().add(end, new InfoHeaderLine(new LinkedHashMap<>() {{
			put("ID", "G_EAS_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in East Asian population (gnomAD)");
		}}));
		header.getHeaderLines().add(end, new InfoHeaderLine(new LinkedHashMap<>() {{
			put("ID", "G_NFE_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in European non Finnish population (gnomAD)");
		}}));
		header.getHeaderLines().add(end, new InfoHeaderLine(new LinkedHashMap<>() {{
			put("ID", "G_FIN_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in Finnish population (gnomAD)");
		}}));
		header.getHeaderLines().add(end, new InfoHeaderLine(new LinkedHashMap<>() {{
			put("ID", "G_OTH_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in other populations (gnomAD)");
		}}));
		header.getHeaderLines().add(end, new InfoHeaderLine(new LinkedHashMap<>() {{
			put("ID", "G_ASJ_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in Ashkenazi Jew population (gnomAD)");
		}}));

	}

	@Override
	public void accept(VariantContext variant, Coordinate grch38) {
		openReader(grch38);
		final VariantContext gnomadVariant = reader.next(grch38);
		// Alleles present in both
		final List<String> alleles = gnomadVariant.getAlternatives().stream()
				.filter(variant.getAlternatives()::contains)
				.collect(Collectors.toList());
		if (alleles.isEmpty()) return;
		for (String allele : alleles) {
			final Info variantInfo = getAlleleInfo(variant, allele);
			final Info gnomadInfo = getAlleleInfo(gnomadVariant, allele);
			for (int i = 0; i < GNOMAD_INFO.size(); i++) {
				String info = GNOMAD_INFO.get(i);
				final String vInfo = VARIANT_INFO.get(i);
				variantInfo.set(vInfo, gnomadInfo.get(info));
			}
		}
	}

	private Info getAlleleInfo(VariantContext variant, String allele) {
		return variant.getInfo().getAllele(variant.indexOfAllele(allele));
	}

	private void openReader(Coordinate coordinate) {
		// Already open
		if (gnomad.isFile()) return;

		// Open only if chromosome changes
		if (openChromosome == null || !openChromosome.equals(coordinate.getChrom())) {
			reader = null;
			// Even if opening the file fails, we keep a reference to the chromosome
			openChromosome = coordinate.getChrom();
			final String filename = String.format(fileName, coordinate.getChrom());
			final File file = new File(gnomad, filename);
			if (!file.exists()) {
				// this can generate lots of warnings
//				Logger.getLogger("variant-utils").warning("No gnomad file for chromosome " + coordinate.getChrom());
				return;
			}
			try {
				reader = new VariantContextReader(FileUtils.getInputStream(file));
			} catch (IOException e) {
				//
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void close() {

	}
}
