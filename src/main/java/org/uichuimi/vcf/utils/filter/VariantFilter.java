package org.uichuimi.vcf.utils.filter;

import org.jetbrains.annotations.NotNull;
import org.uichuimi.vcf.header.DataFormatLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.MultipleVariantReader;
import org.uichuimi.vcf.io.VariantOutput;
import org.uichuimi.vcf.io.VariantWriter;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.utils.common.GenomicProgressBar;
import org.uichuimi.vcf.variant.Info;
import org.uichuimi.vcf.variant.Variant;
import picocli.CommandLine.ArgGroup;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

/**
 * Allows filtering vcf files.
 */
@Command(name = "filter",
		header = "Filters variants in Variant Call Format",
		descriptionHeading = "%n@|bold,underline Description|@:%n",
		description = "Filters variants in Variant Call Format.%n" +
				"In general, two actions can be performed using 'filter' command:%n" +
				" 1) remove unwanted info, from variant (INFO) and genotype (FORMAT) values and%n" +
				" 2) remove variant records (lines) given a series of filters.%n%n@|bold Options:|@",
		sortOptions = false,
		abbreviateSynopsis = true,
		separator = " ",
		usageHelpAutoWidth = true)
@SuppressWarnings({"unused", "FieldMayBeFinal"})
public class VariantFilter implements Callable<Void> {

	@Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
	private boolean version;

	@Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
	private boolean usage;

	@Option(names = {"--verbose", "-v"},
			description = "Set verbose mode. When false, only errors and warnings will be displayed")
	private boolean verbose = false;

	@ArgGroup(validate = false, exclusive = false, heading = "@|bold Input and output options:|@%n")
	private IOOptions io = new IOOptions();

	private static class IOOptions {

		@Option(names = {"-i", "--input"},
				paramLabel = "input",
				description = "Input VCF file (can be compressed with gz or zip). If no input is " +
						"specified, input will be read from standard input (stdin).",
				split = ",")
		private List<File> inputs;
		@Option(names = {"-o", "--output"},
				description = "Output file. If no output is specified, output will be written" +
						" to standard output (stdout).")
		private File output;

		@Option(names = {"--output-format"},
				description = "Output format. If not specified, inferred by output extension." +
						" Supported formats: {${COMPLETION-CANDIDATES}}")
		private FileFormat format;
	}

	@ArgGroup(validate = false,
			exclusive = false,
			heading = "@|bold Sample filtering and selecting options:|@%n")
	private SampleFiltering sf = new SampleFiltering();

	private static class SampleFiltering {
		@Option(names = {"--include-samples"},
				paramLabel = "sample",
				description = "List of samples to be included in the output. " +
						"Incompatible with --exclude-samples",
				split = ",")
		private List<String> includeSamples;

		@Option(names = {"--exclude-samples"},
				paramLabel = "sample",
				description = "List of samples to be excluded in the output. " +
						"Incompatible with --include-samples",
				split = ",")
		private List<String> excludeSamples;

		@Option(names = {"--homozygous"},
				paramLabel = "sample",
				description = "Filters the list of variants to include only those where these " +
						"samples are homozygous for the alternative allele (e.g. 1/1).",
				split = ",")
		private List<String> homozygous;

		@Option(names = {"--heterozygous"},
				paramLabel = "sample",
				description = "Filters the list of variants to include only those where these " +
						"samples are heterozygous for the alternative allele (e.g. 0/1).",
				split = ",")
		private List<String> heterozygous;

		@Option(names = {"--wildtype"},
				paramLabel = "sample",
				description = "Filters the list of variants to include only those where these" +
						" samples are homzygous for the reference allele (e.g. 0/0).",
				split = ",")
		private List<String> wildtype;

		@Option(names = {"--uncalled"},
				paramLabel = "sample",
				description = "Specifies which samples of the specified in the heterozygous, " +
						"homozygous and wildtype can be uncalled, i. e., without data. By " +
						"default, this is not allowed.")
		private List<String> uncalled;
	}

	@Option(names = {"-f", "--filter"},
			paramLabel = "pattern",
			arity = "1..*",
			split = " ",
			description = "Adds an INFO field filter in the form: %n" +
					"\t[<SAMPLE>.]<KEY>[<FLAGS>]<OPERATOR><VALUE>%n" +
					"@|bold SAMPLE:|@ is optional and must be followed by a dot, the filter " +
					"will be applied to a FORMAT tag only for that sample. You can also leave" +
					" it blank (just type the dot) and it will be applied to all samples or " +
					"use an asterisk (*) to pass if any of the samples passes.%n" +
					"@|bold KEY:|@ must be one of INFO ids. Special keys are CHROM, POS," +
					" FILTER and QUAL%n" +
					"@|bold FLAGS:|@ A combination of ? and *. The ? flag means that null " +
					"values (not present) are allowed. The * is used with array fields " +
					"(A, G, R, . or N>1): if specified, it is enough with one value passing" +
					" the filter, otherwise, all values in the array must match." +
					"@|bold operator:|@ a math (<, <=, >, >=, =) or string (= equals, <> " +
					"distinct, ~ contains) " +
					"operator.%n" +
					"@|bold value:|@ is the comparing value for the specified key, usually a" +
					" number or string. operator and value are optional for boolean keys.%n" +
					"You can enforce a filter to be mandatory by prefixing it with an exclamation" +
					" mark !. This will enforce the filter to actively match, i.e. does not " +
					"accept null values.%n" +
					"@|underline Examples:|@%n" +
					"\t@|yellow -f|@ DP>5 (result will contain only variants which DP is greater" +
					" than 5)%n" +
					"\t@|yellow -f|@ SIFTp?tolerated (filters variants which SIFT contains " +
					"'tolerated')%n" +
					"\t@|yellow -f|@ SYMBOL=LDLR (filter variants with SYMBOL = LDLR, if SYMBOL " +
					"is an array, all values must be LDLR)%n" +
					"\t@|yellow -f|@ EX_AF*<0.01 (filters variants with at least one ExAC" +
					" frequency below 0.01)%n" +
					"\t@|yellow -f|@ .DP>5 (filters variants where ALL samples have DP greater" +
					" than 5)%n" +
					"\t@|yellow -f|@ NA001.GQ>=10 (sample NA001 must have GQ greater or equal to" +
					" 10)%n" +
					"\t@|yellow -f|@ DB (filters variants with DB flag)%n" +
					"\t@|yellow -f|@ SYMBOL=LDLR (only variants with symbol name LDLR, variants" +
					" without symbol are not included.)")
	private List<String> patterns;

	private final List<Filter> filters = new ArrayList<>();

	private PrintStream log = System.out;

	VariantFilter() {
	}

	@Override
	public Void call() throws Exception {
		final GenomicProgressBar bar = new GenomicProgressBar();
		if (io.output == null) log = System.err;
		if (sf.includeSamples != null && sf.excludeSamples != null) {
			log.println("ERROR: Incompatible options --include-samples and --exclude-samples");
			return null;
		}
		long line = 0;
		long passed = 0;
		// Use standard output if no output is provided
		final OutputStream out;
		if (io.output == null) out = System.out;
		else out = FileUtils.getOutputStream(io.output);

		//  Use standard input if no input is provided
		final List<InputStream> in = new ArrayList<>();
		if (io.inputs != null) {
			if (verbose) log.println(String.format("Detected %d input files: ", io.inputs.size()));
			for (File file : io.inputs) {
				in.add(FileUtils.getInputStream(file));
				if (verbose) log.println(" - " + file);
			}
		} else in.add(System.in);

		Variant variant = null;
		VariantOutput writer = getVariantOutput(out);
		try (MultipleVariantReader reader = new MultipleVariantReader(in); writer) {
			// Create filters
			createFilters(reader.getHeader());
			final VcfHeader writerHeader = new VcfHeader(reader.getHeader());
			setSamples(writerHeader);
			writer.setHeader(writerHeader);
			if (log == System.out && verbose) bar.start();
			final Iterator<Variant> iterator = reader.iterator();
			while (iterator.hasNext()) {
				line += 1;
				variant = iterator.next();
				if (applyFilters(variant)) {
					writer.write(convert(variant, writerHeader));
					passed += 1;
				}
				if (log == System.out && verbose)
					bar.update(variant.getCoordinate(), String.format("%s %,12d", variant.getCoordinate().getChrom(), variant.getCoordinate().getPosition()));
//				if (line == 10000) break;
			}
		} catch (Exception e) {
			throw new Exception(String.format("At line %d, variant %s ", line, variant), e);
		}
		if (log == System.out && verbose) bar.stop();
		if (verbose) log.printf("Read %d variants, %d passed%n", line, passed);
		return null;
	}

	@NotNull
	private VariantOutput getVariantOutput(OutputStream out) {
		if (io.format == null) {
			if (io.output == null) {
				return new VariantWriter(out);
			}
			if (io.output.getName().endsWith(".vcf") || io.output.getName().endsWith(".vcf.gz")) {
				return new VariantWriter(out);
			} else {
				return new TsvWriter(out);
			}
		}
		if (io.format == FileFormat.TSV) {
			return new TsvWriter(out);
		} else {
			return new VariantWriter(out);
		}
	}

	/**
	 * Clones variant using new header
	 */
	private Variant convert(Variant variant, VcfHeader header) {
		final Variant copy = new Variant(header, variant.getCoordinate(), variant.getReferences(), variant.getAlternatives());
		copy.setQuality(variant.getQuality());
		copy.getFilters().addAll(variant.getFilters());
		copy.getIdentifiers().addAll(variant.getIdentifiers());
		variant.getInfo().forEach(copy::setInfo);
		List<String> samples = header.getSamples();
		for (int i = 0; i < samples.size(); i++) {
			final String sample = samples.get(i);
			final int from = variant.getHeader().getSamples().indexOf(sample);
			final Info fromInfo = variant.getSampleInfo().get(from);
			if (fromInfo != null) {
				final Info target = copy.getSampleInfo(i);
				fromInfo.forEach(target::set);
			}
		}
		return copy;
	}

	private void setSamples(VcfHeader header) {
		if (sf == null) return;
		final int size = header.getSamples().size();
		if (sf.includeSamples != null)
			header.getSamples().retainAll(sf.includeSamples);
		else if (sf.excludeSamples != null)
			header.getSamples().removeAll(sf.excludeSamples);
		if (verbose)
			log.println(String.format("Found %d samples. Sending to output %d: %s",
					size, header.getSamples().size(), header.getSamples()));
	}

	private void createFilters(VcfHeader header) {
		createGenotypeFilter(header);
		createVariantFilters(header);
	}

	private void createGenotypeFilter(VcfHeader header) {
		final Map<String, Set<GT>> gts = new TreeMap<>();
		// genotype filters
		mapGenotype(header, gts, sf.homozygous, GT.HOMO);
		mapGenotype(header, gts, sf.heterozygous, GT.HETERO);
		mapGenotype(header, gts, sf.wildtype, GT.WILD);
		mapGenotype(header, gts, sf.uncalled, GT.UNCALLED);
		filters.add(new GenotypeFilter(header, gts));
		if (verbose) {
			if (!gts.isEmpty()) log.printf("Filtering genotypes:%n");
			gts.forEach((sample, set) -> log.printf(" - %s: %s%n", sample, set));
		}
	}

	private void mapGenotype(VcfHeader header, Map<String, Set<GT>> gts, List<String> samples, GT gt) {
		if (samples != null && !samples.isEmpty()) {
			for (String sample : samples) {
				if (header.getSamples().contains(sample)) {
					gts.computeIfAbsent(sample, s -> EnumSet.noneOf(GT.class)).add(gt);
				} else log.printf("WARNING: sample %s not found in file header%n", sample);
			}
		}
	}

	private static final Pattern PATTERN = Pattern.compile("" +
			"(?:(?<sample>\\w*|\\*)\\.)?" +
			"(?<key>\\w+)" +
			"(?<nulls>\\?)?" +
			"(?<any>\\*)?" +
			"(?:(?<operator><>|>=|<=|>|<|~|=)" +
			"(?<value>.+))?"
	);

	private Filter createFilter(String pattern, VcfHeader header) {
		final Matcher matcher = PATTERN.matcher(pattern);
		if (!matcher.matches()) {
			log.printf("ERROR: Invalid filter format: %s%n", pattern);
			System.exit(1);
		}
		final String nulls = matcher.group("nulls");
		final String sample = matcher.group("sample");
		final String key = matcher.group("key");
		final String any = matcher.group("any");
		final String operator = matcher.group("operator");
		final String value = matcher.group("value");
		final boolean matchAll = any == null;
		final boolean acceptNulls = nulls != null;
		final Operator op = Operator.getInstance(operator);

		final DataFormatLine headerLine;
		if (sample == null) {
			// INFO
			if (value == null)
				return new InfoFilter(key, Operator.EQ, true, matchAll, acceptNulls);
			if (key.equals("CHROM"))
				return new ChromosomeFilter(value, op);
			if (key.equals("POS"))
				return new PositionFilter(Long.parseLong(value), op);
			if (key.equals("QUAL"))
				return new QualityFilter(Double.parseDouble(value), op);
			if (key.equals("FILTER"))
				return new FilterFilter(value, op, matchAll);
			if (!header.hasComplexHeader("INFO", key))
				log.printf("WARNING: INFO %s not found (interpreting as String)%n", key);
			headerLine = header.getInfoHeader(key);
			Object val = headerLine.getProperty(value).getValue();
			if (val instanceof List) val = ((List) val).iterator().next();
			return new InfoFilter(key, op, val, matchAll, acceptNulls);
		} else {
			// FORMAT
			if (value == null)
				return new SampleFilter(header, sample, key, Operator.EQ, true, matchAll, acceptNulls);
			if (!header.hasComplexHeader("FORMAT", key))
				log.printf("WARNING: FORMAT %s not found (interpreting as String)%n", key);
			headerLine = header.getFormatHeader(key);
			Object val = headerLine.getProperty(value).getValue();
			if (val instanceof List) val = ((List) val).iterator().next();
			return new SampleFilter(header, sample, key, op, val, matchAll, acceptNulls);
		}
	}

	private void createVariantFilters(VcfHeader header) {
		if (patterns == null) return;
		final List<Filter> filterList = new ArrayList<>();
		for (String pattern : patterns) filterList.add(createFilter(pattern, header));
		if (!filterList.isEmpty()) {
			if (verbose) {
				log.println("Filters:");
				for (Filter filter : filterList) log.println(" - " + filter);
			}
			filters.addAll(filterList);
		}
	}


	private boolean applyFilters(Variant variant) {
		return filters.stream().allMatch(filter -> filter.filter(variant));
	}

	enum GT {
		HOMO, HETERO, WILD, UNCALLED
	}

	enum Operator {
		EQ("=", Object::equals),
		GT(">", (a, b) -> ((Number) a).doubleValue() > ((Number) b).doubleValue()),
		LT("<", (a, b) -> ((Number) a).doubleValue() < ((Number) b).doubleValue()),
		GE(">=", (a, b) -> ((Number) a).doubleValue() >= ((Number) b).doubleValue()),
		LE("<=", (a, b) -> ((Number) a).doubleValue() <= ((Number) b).doubleValue()),
		NEQ("<>", (a, b) -> !a.equals(b)),
		IN("~", (a, b) -> ((String) a).contains((String) b));

		private final String symbol;
		private final BiFunction<? super Object, ? super Object, Boolean> operation;

		static Operator getInstance(String symbol) {
			for (Operator operator : Operator.values()) {
				if (operator.symbol.equals(symbol)) return operator;
			}
			return null;
		}

		Operator(String symbol, BiFunction<? super Object, ? super Object, Boolean> operation) {
			this.symbol = symbol;
			this.operation = operation;
		}

		public String getSymbol() {
			return symbol;
		}

		boolean apply(Object a, Object b) {
			return operation.apply(a, b);
		}
	}

	private static enum FileFormat {
		TSV, VCF
	}
}
