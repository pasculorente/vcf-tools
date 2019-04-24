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
import java.util.stream.Collectors;

public abstract class FrequencyAnnotator implements VariantConsumer {

	private File path;
	private VariantContextReader reader;
	private String openChromosome;
	private final Collection<FrequencyAnnotation> annotations;

	FrequencyAnnotator(File file) {
		this.annotations = getAnnotations();
		if (!file.exists()) throw new IllegalArgumentException(file + " does not exist");
		if (file.isFile()) {
			try {
				reader = new VariantContextReader(FileUtils.getInputStream(file));
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot open file", e);
			}
		} else {
			this.path = file;
		}
	}

	protected abstract Collection<FrequencyAnnotation> getAnnotations();

	@Override
	public void start(VcfHeader header) {
		injectHeaderLines(header);
	}

	private void injectHeaderLines(VcfHeader header) {
		int pos = getLastInfoIndex(header);
		final Collection<InfoHeaderLine> headerLines = new ArrayList<>();
		for (FrequencyAnnotation annotation : annotations)
			headerLines.add(new InfoHeaderLine(annotation.targetId, "A", "Float", annotation.description));
		header.getHeaderLines().addAll(pos, headerLines);
	}

	private static int getLastInfoIndex(VcfHeader header) {
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
		return end;
	}

	@Override
	public void accept(VariantContext variant, Coordinate grch38) {
		openReader(grch38);
		final VariantContext annotated = reader.next(grch38);
		// Alleles present in both
		final List<String> alleles = annotated.getAlternatives().stream()
				.filter(variant.getAlternatives()::contains)
				.collect(Collectors.toList());
		if (alleles.isEmpty()) return;
		for (String allele : alleles) {
			final Info variantInfo = getAlleleInfo(variant, allele);
			final Info annotatedInfo = getAlleleInfo(annotated, allele);
			for (FrequencyAnnotation annotation : annotations)
				variantInfo.set(annotation.targetId, annotatedInfo.get(annotation.sourceId));
		}
	}

	private Info getAlleleInfo(VariantContext variant, String allele) {
		return variant.getInfo().getAllele(variant.indexOfAllele(allele));
	}


	private void openReader(Coordinate coordinate) {
		// It's a file, no need to open readers
		if (path == null) return;

		// Open only if chromosome changes
		if (openChromosome == null || !openChromosome.equals(coordinate.getChrom())) {
			reader = null;
			// Even if opening the file fails, we keep a reference to the chromosome
			openChromosome = coordinate.getChrom();
			final String filename = getFileName(coordinate.getChrom());
			final File file = new File(path, filename);
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
		try {
			if (reader != null) reader.close();
		} catch (IOException ignored) {
		}
	}

	protected abstract String getFileName(String chrom);

	class FrequencyAnnotation {
		private final String sourceId;
		private final String targetId;
		private final String description;

		FrequencyAnnotation(String sourceId, String targetId, String description) {
			this.sourceId = sourceId;
			this.targetId = targetId;
			this.description = description;
		}
	}

}

