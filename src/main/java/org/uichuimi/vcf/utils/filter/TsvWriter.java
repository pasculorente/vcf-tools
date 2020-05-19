package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.header.ComplexHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.VariantOutput;
import org.uichuimi.vcf.variant.Info;
import org.uichuimi.vcf.variant.Variant;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TsvWriter implements VariantOutput {

	private static final List<String> COLUMNS = List.of("Chromosome", "Position", "id", "Reference", "Alternative", "Quality", "Gene");
	private static final String ARRAY_DELIMITER = ",";
	private static final NumberFormat QUALITY_FORMAT = new DecimalFormat();
	public static final String SYMBOL = "SYMBOL";

	private Function<Variant, String> geneExtractor;

	static {
		QUALITY_FORMAT.setMaximumFractionDigits(3);
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
		final ComplexHeaderLine symbolHeader = header.getComplexHeader("INFO", SYMBOL);
		if (symbolHeader == null) {
			geneExtractor = variant -> variant.getInfo(SYMBOL) == null ? "" : variant.getInfo(SYMBOL);
		} else if (symbolHeader.getValue("Number").equals("1")) {
			geneExtractor = variant -> variant.getInfo(SYMBOL) == null ? "" : variant.getInfo(SYMBOL);
		} else {
			geneExtractor = variant -> variant.getInfo(SYMBOL) == null
					? ""
					: variant.<List<String>>getInfo(SYMBOL).stream().distinct().collect(Collectors.joining(ARRAY_DELIMITER));
		}
	}

	@Override
	public void write(Variant variant) {
		final List<String> values = new ArrayList<>(columns.size());
		values.add(variant.getCoordinate().getChrom());
		values.add(String.valueOf(variant.getCoordinate().getPosition()));
		values.add(String.join(ARRAY_DELIMITER, variant.getIdentifiers()));
		values.add(String.join(ARRAY_DELIMITER, variant.getReferences()));
		values.add(String.join(ARRAY_DELIMITER, variant.getAlternatives()));
		values.add(QUALITY_FORMAT.format(variant.getQuality()));
		values.add(geneExtractor.apply(variant));
		for (Info info : variant.getSampleInfo()) values.add(info == null ? "" : info.get("GT"));
		output.println(String.join(delimiter, values));
	}

	@Override
	public void close() throws Exception {
		output.close();
	}
}
