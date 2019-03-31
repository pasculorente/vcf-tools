package org.uichuimi.vcf.utils.neo4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TableWriter {

	private static final String NULL_VALUE = "";
	private final static String DEFAULT_DELIMITER = "\t";

	private final String delimiter;
	private final BufferedWriter writer;
	private List<String> columns;
	private List<Function<List<?>, Comparable<?>>> functions = new ArrayList<>();
	private List<Set<Comparable<?>>> indexes = new ArrayList<>();

	/**
	 * List of registered writers
	 */
	private static final Collection<TableWriter> writers = new ArrayList<>();

	static {
		// Auto stop all writers at shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(() -> writers.forEach(TableWriter::close)));
	}

	public TableWriter(File file, List<String> columns) throws IOException {
		this(file, DEFAULT_DELIMITER, columns);
		writers.add(this);
	}

	public TableWriter(File file, String delimiter, List<String> columns) throws IOException {
		this.delimiter = delimiter;
		this.writer = new BufferedWriter(new FileWriter(file));
		this.columns = columns;
		write(columns);
	}

	public void createIndex(int columnIndex) {
		createIndex(objects -> (Comparable<?>) objects.get(columnIndex));
	}

	public void createIndex(Function<List<?>, Comparable<?>> indexFunction) {
		functions.add(indexFunction);
		indexes.add(new HashSet<>());
	}

	public void write(Object... values) throws IOException {
		write(Arrays.asList(values));
	}

	public void write(List<?> values) throws IOException {
		if (values.size() != columns.size()) {
			Logger.getLogger(getClass().getName()).warning(String.format("(%d) %s must contain %d values (%s)", values.size(), values, columns.size(), columns));
		}
		final List<Comparable<?>> ks = new ArrayList<>();
		for (int i = 0; i < functions.size(); i++) {
			final Comparable<?> key = functions.get(i).apply(values);
			if (indexes.get(i).contains(key)) return;
			ks.add(key);
		}
		for (int i = 0; i < ks.size(); i++) indexes.get(i).add(ks.get(i));

		final String line = values.stream()
				.map(o -> o == null ? NULL_VALUE : o)
				.map(String::valueOf)
				.collect(Collectors.joining(delimiter));
		writer.write(line);
		writer.newLine();
	}

	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}