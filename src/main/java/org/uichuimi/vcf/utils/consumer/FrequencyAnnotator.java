package org.uichuimi.vcf.utils.consumer;

import org.jetbrains.annotations.NotNull;
import org.uichuimi.vcf.header.InfoHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class FrequencyAnnotator implements VariantConsumer {

	private File path;
	private VariantReader reader;
	private String openChromosome;
	private final Collection<FrequencyAnnotation> annotations;

	FrequencyAnnotator(File file) {
		this.annotations = getAnnotations();
		if (!file.exists()) throw new IllegalArgumentException(file + " does not exist");
		if (file.isFile()) {
			try {
				reader = new VariantReader(FileUtils.getInputStream(file));
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
		for (FrequencyAnnotation annotation : annotations)
			header.addHeaderLine(new InfoHeaderLine(annotation.targetId, "A", "Float", annotation.description));
	}

	@Override
	public void accept(Variant variant, Coordinate grch38) {
		openReader(grch38.getChrom());
		if (reader == null) return;
		final Variant annotated = reader.next(grch38);
		if (annotated == null) return;
		// Find common alleles and store indexes
		final Map<Integer, Integer> map = getAlleleMap(variant, annotated);
		if (map.isEmpty()) return;
		for (FrequencyAnnotation annotation : annotations) {
			final List<Float> af = annotated.getInfo(annotation.sourceId);
			if (af == null) continue;
			final Float[] frs = new Float[variant.getAlternatives().size()];
			map.forEach((v, a) -> frs[v] = af.get(a));
			variant.setInfo(annotation.targetId, Arrays.asList(frs));
		}
	}

	@NotNull
	private Map<Integer, Integer> getAlleleMap(Variant variant, Variant annotated) {
		final Map<Integer, Integer> index = new HashMap<>();
		final List<String> alternatives = variant.getAlternatives();
		for (int v = 0; v < alternatives.size(); v++) {
			final String alternative = alternatives.get(v);
			final int a = annotated.getAlternatives().indexOf(alternative);
			if (a < 0) continue;
			index.put(v, a);
		}
		return index;
	}

	private void openReader(String chrom) {
		// It's a file, no need to open readers
		if (path == null) return;

		// Open only if chromosome changes
		if (openChromosome == null || !openChromosome.equals(chrom)) {
			reader = null;
			// Even if opening the file fails, we keep a reference to the chromosome
			openChromosome = chrom;
			final String filename = getFileName(chrom);
			final File file = new File(path, filename);
			if (!file.exists()) {
				// this can generate lots of warnings
//				Logger.getLogger("variant-utils").warning("No gnomad file for chromosome " + coordinate.getChrom());
				return;
			}
			try {
				reader = new VariantReader(FileUtils.getInputStream(file));
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

	static class FrequencyAnnotation {
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
