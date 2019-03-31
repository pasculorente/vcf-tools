package org.uichuimi.vcf.utils.kgenomes;

import org.uichuimi.vcf.header.InfoHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.input.VariantContextReader;
import org.uichuimi.vcf.utils.annotate.VariantConsumer;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Info;
import org.uichuimi.vcf.variant.VariantContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class KGenomesAnnotator implements VariantConsumer {

	private final VariantContextReader reader;

	public KGenomesAnnotator(InputStream inputStream) {
		reader = new VariantContextReader(inputStream);
	}

	public KGenomesAnnotator(File file) throws IOException {
		this(FileUtils.getInputStream(file));
	}

	@Override
	public void start(VcfHeader header) {
		addAnnotationHeaders(header);
	}

	private void addAnnotationHeaders(VcfHeader header) {
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
		header.getHeaderLines().add(end, new InfoHeaderLine(new LinkedHashMap<String, String>() {{
			put("ID", "AFR_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in African population (1000 Genomes Phase 3)");
		}}));
		header.getHeaderLines().add(end, new InfoHeaderLine(new LinkedHashMap<String, String>() {{
			put("ID", "AMR_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in American population (1000 Genomes Phase 3)");
		}}));
		header.getHeaderLines().add(end, new InfoHeaderLine(new LinkedHashMap<String, String>() {{
			put("ID", "EAS_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in East Asian population (1000 Genomes Phase 3)");
		}}));
		header.getHeaderLines().add(end, new InfoHeaderLine(new LinkedHashMap<String, String>() {{
			put("ID", "EUR_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in European population (1000 Genomes Phase 3)");
		}}));
		header.getHeaderLines().add(end, new InfoHeaderLine(new LinkedHashMap<String, String>() {{
			put("ID", "SAS_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in South Asian population (1000 Genomes Phase 3)");
		}}));
	}

	@Override
	public void accept(VariantContext variant, Coordinate coordinate) {
		final VariantContext kgAnnotation = reader.next(coordinate);
		annotateFrequencies(variant, kgAnnotation);
	}

	private void annotateFrequencies(VariantContext variant, VariantContext kgVariant) {
		if (kgVariant == null) return;
		final List<String> alternatives = kgVariant.getAlternatives();
		for (int i = 0; i < alternatives.size(); i++) {
			final String allele = alternatives.get(i);
			final int index = variant.getAlternatives().indexOf(allele);
			if (index >= 0) {
				final Info source = kgVariant.getInfo().getAlternativeAllele(i);
				final Info target = variant.getInfo().getAlternativeAllele(index);
				for (String pop : Arrays.asList("EAS_AF", "SAS_AF", "EUR_AF", "AFR_AF", "AMR_AF"))
					target.set(pop, source.get(pop));
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
