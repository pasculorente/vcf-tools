package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.VariantOutput;
import org.uichuimi.vcf.utils.annotation.AnnotationConstants;
import org.uichuimi.vcf.variant.Info;
import org.uichuimi.vcf.variant.Variant;
import org.uichuimi.vcf.variant.VcfConstants;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TsvWriter implements VariantOutput {

	private static final List<String> KEYS = List.of("KG_AF", "GG_AF", "GE_AF", "EX_AF");
	private static final List<String> COLUMNS = List.of("Chromosome", "Position", "id", "Reference",
			"Alternative", "Quality", "Gene", "Consequence", "Type", "GMAF", "Sift", "Polyphen");
	private static final String ARRAY_DELIMITER = ",";
	private static final NumberFormat QUALITY_FORMAT = new DecimalFormat();
	public static final String SYMBOL = "SYMBOL";

	static {
		QUALITY_FORMAT.setMaximumFractionDigits(3);
		QUALITY_FORMAT.setGroupingUsed(false);
	}

	private final PrintStream output;

	private final String delimiter;
	private List<String> columns;

	public TsvWriter(OutputStream outputStream) {
		this(outputStream, "\t");
	}

	public TsvWriter(OutputStream outputStream, String delimiter) {
		output = new PrintStream(outputStream);
		this.delimiter = delimiter;
	}

	@Override
	public void setHeader(VcfHeader header) {
		columns = new ArrayList<>(COLUMNS);
		columns.addAll(header.getSamples());
		output.println(String.join(delimiter, columns));
	}

	@Override
	public void write(Variant variant) {
		final List<String> values = new ArrayList<>(columns.size());
		values.add(variant.getCoordinate().getChrom());
		values.add(String.valueOf(variant.getCoordinate().getPosition()));
		values.add(String.join(ARRAY_DELIMITER, variant.getIdentifiers()));
		values.add(String.join(ARRAY_DELIMITER, variant.getReferences()));
		values.add(String.join(ARRAY_DELIMITER, variant.getAlternatives()));
		values.add(variant.getQuality() == null ? "" : QUALITY_FORMAT.format(variant.getQuality()));
		values.add(extractInfo(variant, SYMBOL));
		values.add(extractInfo(variant, "CONS"));
		values.add(extractInfo(variant, "FT"));
		values.add(extractGmaf(variant));
		values.add(extractInfo(variant, "Sift"));
		values.add(extractInfo(variant, "Polyphen"));
		for (Info info : variant.getSampleInfo()) {
			final String gt = info.get("GT");
			values.add(gt == null ? "" : gt);
		}
		output.println(String.join(delimiter, values));
	}

	private String extractGmaf(Variant variant) {
		Double gmaf = null;
		for (String key : KEYS) {
			final Double f = writeFrequencies(variant, key);
			if (f != null) {
				if (gmaf == null) gmaf = f;
				else gmaf = Double.max(gmaf, f);
			}
		}
		return gmaf == null ? "" : QUALITY_FORMAT.format(gmaf);
	}

	private Double writeFrequencies(Variant variant, String key) {
		// Frequencies are Number=A, so a must be position in alternatives
		final List<String> freqs = variant.getInfo(key);
		if (freqs == null) return null;
		if (freqs.size() <= 0) return null;
		final String fr = freqs.get(0);
		if (fr == null || fr.equals(VcfConstants.EMPTY_VALUE)) return null;
		final String[] values = fr.split(AnnotationConstants.ESCAPED_DELIMITER);
		Double max = null;
		for (String value : values) {
			max = Double.max(max == null ? 0 : max, Double.parseDouble(value));
		}
		return max;
	}


	@Override
	public void close() throws Exception {
		output.close();
	}

	private String extractInfo(Variant variant, String key) {
		final Object value = variant.getInfo(key);
		if (value == null) return "";
		if (value instanceof Collection)
			return ((Collection<?>) value).stream()
					.filter(Objects::nonNull)
					.map(String::valueOf)
					.collect(Collectors.joining(ARRAY_DELIMITER));
		return String.valueOf(value);
	}

}
