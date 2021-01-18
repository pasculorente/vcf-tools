package org.uichuimi.vcf.utils.dbsnp;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.ArrayUtils;
import org.uichuimi.vcf.header.InfoHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.utils.FileUtils;
import org.uichuimi.vcf.utils.annotation.consumer.VariantConsumer;
import org.uichuimi.vcf.utils.exception.VcfException;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DbNSPFPAnnotator implements VariantConsumer {

	private static final Logger logger = Logger.getLogger(DbNSPFPAnnotator.class.getName());


	private final List<String> fields;
	private final String tag;

	private final LineIterator inputStream;
	private String[] next;
	private Map<String, Integer> index;
	private int transcriptIndex;

	private BiFunction<String, Integer, Object> transformer;
	private Function<Variant, String> transcriptExtractor;
	private Function<Variant, Integer> indexExtractor;
	private Coordinate coordinate;

	/**
	 * -l list of fields to annotate
	 * -t first|all|tag, default all
	 *
	 * @param dbnsfp dbnsfp file
	 * @param fields list of fields to annotate
	 * @param tag    whether to annotate first, all or values indexed by transcript
	 * @throws IOException if dbnsfp is unreadable
	 */
	public DbNSPFPAnnotator(File dbnsfp, List<String> fields, String tag) throws IOException {
		inputStream = IOUtils.lineIterator(FileUtils.getBufferedReader(dbnsfp));
		this.fields = fields;
		this.tag = tag;
	}

	@Override
	public void start(VcfHeader header) throws VcfException {
		// Check tag argument
		// Sets 'Number' in INFO fields and transformer function
		final String number;
		if (tag.equalsIgnoreCase("all")) {
			number = ".";
			transformer = (value, index) -> List.of(value.split(";"));
		} else if (tag.equalsIgnoreCase("first")) {
			number = "1";
			transformer = (value, index) -> value.split(";")[0];
		} else {
			if (!header.hasComplexHeader("INFO", tag)) {
				final String msg = String.format("VCF file does not contain field %s", tag);
				logger.severe(msg);
				throw new VcfException(msg);
			} else {
				number = "1";
				transformer = (value, index) -> index >= 0 ? value.split(";")[index] : ".";
				final String nbr = header.getInfoHeader(tag).getNumber();
				if (nbr.equals("1")) {
					transcriptExtractor = variant -> variant.getInfo(tag);
				} else {
					transcriptExtractor = variant -> {
						final List<String> tr = variant.getInfo(tag);
						if (tr == null || tr.isEmpty()) return null;
						else return tr.get(0);
					};
				}
			}
		}
		// Load only required INFO fields
		final InputStream resource = getClass().getResourceAsStream("/dbnsfp.txt");
		Map<String, InfoHeaderLine> headerLines;
		try {
			final Collection<InfoHeaderLine> infoHeaderLines = new ArrayList<>();
			for (String line : IOUtils.readLines(resource, Charset.defaultCharset())) {
				final String[] split = line.split(",");
				final String field = split[0];
				if (fields.contains(field)) {
					infoHeaderLines.add(new InfoHeaderLine(field, number, split[1], "dbNSFP " + field));
				}
			}
			headerLines = infoHeaderLines.stream().collect(Collectors.toMap(InfoHeaderLine::getId, Function.identity()));
		} catch (IOException e) {
			e.printStackTrace();
			headerLines = Map.of();
		}
		for (String field : fields) {
			if (!headerLines.containsKey(field)) {
				logger.warning(String.format("%s is not a valid dbNSFP field", field));
			}
			if (header.hasComplexHeader("INFO", field)) {
				logger.warning(String.format("Field %s is already in VCF header", field));
			} else {
				header.addHeaderLine(headerLines.get(field));
			}
		}
		// Read header
		index = new TreeMap<>();
		String[] columns = inputStream.next().split("\t");
		for (int i = 0; i < columns.length; i++) {
			String column = columns[i];
			if (fields.contains(column)) {
				index.put(column, i);
			}
			if (column.equals("Ensembl_transcriptid")) transcriptIndex = i;
		}
	}

	@Override
	public void accept(Variant variant) {
		// Advance annotations till variant pos
		String[] annotation = next(variant);
		while (annotation != null) {
			// Check ref and alt
			if (variant.getReferences().get(0).equals(annotation[2]) && variant.getAlternatives().contains(annotation[3])) {
				annotate(annotation, variant);
			}
			annotation = next(variant);
		}

	}

	private void annotate(String[] annotation, Variant variant) {
		int trIdx;
		if (!tag.equals("first") && !tag.equals("all")) {
			final String[] transcripts = annotation[transcriptIndex].split(";");
			String transcript = transcriptExtractor.apply(variant);
			if (transcript != null && transcript.contains(".")) {
				// dbNSFP uses raw ENST, with no version
				transcript = transcript.split("\\.")[0];
			}
			trIdx = ArrayUtils.indexOf(transcripts, transcript);
		} else {
			trIdx = -1;
		}
		index.forEach((field, idx) -> {
			final String value = annotation[idx];
			if (value.equals(".")) return;
			final Object transformed = transformer.apply(value, trIdx);
			variant.setInfo(field, transformed);
		});

	}

	private String[] next(Variant variant) {
		while (true) {
			if (next != null) {
				final int compare = coordinate.compareTo(variant.getCoordinate());
				if (compare == 0) {
					// Oh, we already have a match
					String[] rtn = next;
					next = null;
					return rtn;
				} else if (compare > 0) {
					// Ok, your variant is way below our current annotation
					return null;
				}
				// Annotations must advance
			}
			if (!inputStream.hasNext()) {
				next = null;
				return null;
			}
			next = inputStream.next().split("\t");
			coordinate = new Coordinate(next[0], Long.parseLong(next[1]));
		}
	}

	@Override
	public void close() {
		try {
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
