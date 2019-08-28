package org.uichuimi.vcf.utils.annotation.consumer;

import org.jetbrains.annotations.NotNull;
import org.uichuimi.vcf.header.InfoHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.annotation.AnnotationConstants;
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

public abstract class FrequencyAnnotator implements VariantConsumer {

	private static final NumberFormat DECIMAL = new DecimalFormat("#.####");

	private File path;
	private VariantReader reader;
	private String openChromosome;

	FrequencyAnnotator(File file) {
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

	abstract String getPrefix();

	abstract String getDatabaseName();

	abstract List<String> getPopulations();

	abstract List<String> getKeys();

	@Override
	public void start(VcfHeader header) {
		injectHeaderLines(header);
	}

	private void injectHeaderLines(VcfHeader header) {
		final String description = String.format("Allele frequency for %s (%s)", getDatabaseName(), String.join(AnnotationConstants.DELIMITER, getPopulations()));
		header.addHeaderLine(new InfoHeaderLine(getPrefix() + "_AF", VcfConstants.NUMBER_A, VcfType.STRING, description));
	}

	@Override
	public void accept(Variant variant, Coordinate grch38) {
		openReader(grch38.getChrom());
		if (reader == null) return;
		final Variant annotated = reader.next(grch38);
		if (annotated == null) return;
		final double[][] fr = createFrequencies(variant, annotated);
		if (fr == null) return;
		writeFrequencies(variant, fr, getPrefix() + "_AF");
	}

	/**
	 * Create a matrix with dimensions (a,p), where <em>a</em> is the number of alternatives alleles
	 * and <em>p</em> the number of populations with all the given frequencies. Result can be null
	 * if no data is available.
	 *
	 * @param variant
	 * 		variant to be annotated
	 * @param annotated
	 * 		source variant with frequency values
	 * @return a matrix with the frequencies indexed by allele and population
	 */
	double[][] createFrequencies(Variant variant, Variant annotated) {
		final Map<Integer, Integer> map = getAlleleMap(variant, annotated);
		if (map.isEmpty()) return null;
		final List<String> populations = getPopulations();
		final List<String> keys = getKeys();
		final double[][] fr = new double[variant.getAlternatives().size()][populations.size()];
		for (double[] doubles : fr) Arrays.fill(doubles, -1);
		for (int p = 0; p < keys.size(); p++) {
			final List<Float> af = annotated.getInfo(keys.get(p));
			if (af == null) continue;
			int finalP = p;
			map.forEach((v, a) -> fr[v][finalP] = af.get(a));
		}
		return fr;
	}

	private void writeFrequencies(Variant variant, double[][] fr, String key) {
		final List<String> ex_af = new ArrayList<>(variant.getAlternatives().size());
		for (double[] freq : fr) {
			// if any of the frequencies is available, the rest goes to null
			if (Arrays.stream(freq).anyMatch(v -> v >= 0)) {
				final StringJoiner joiner = new StringJoiner(AnnotationConstants.DELIMITER);
				for (double v : freq) {
					if (v >= 0) joiner.add(DECIMAL.format(v));
					else joiner.add(VcfConstants.EMPTY_VALUE);
				}
				ex_af.add(joiner.toString());
			} else {
				ex_af.add(VcfConstants.EMPTY_VALUE);
			}
		}
		if (ex_af.stream().anyMatch(s -> !s.equals(VcfConstants.EMPTY_VALUE)))
			variant.setInfo(key, ex_af);
	}

	/**
	 * Computes the index relation between variant and annotation alternative alleles.
	 *
	 * @param variant
	 * 		variant to be annotated
	 * @param annotated
	 * 		variant containing the annotations
	 * @return a map of indices between variant and annotation alternative alleles
	 */
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

}
