package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.MultipleVariantReader;
import org.uichuimi.vcf.io.VariantWriter;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.utils.common.GenomicProgressBar;
import org.uichuimi.vcf.variant.Variant;
import picocli.CommandLine.ArgGroup;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

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
public class VariantFilter implements Callable<Void> {

	@Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
	private boolean version;

	@Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
	private boolean usage;

	@ArgGroup(validate = false, exclusive = false, heading = "@|bold Input and output options:|@%n")
	private IOOptions io = new IOOptions();

	private static class IOOptions {

		@Option(names = {"-i", "--input"},
				description = "Input VCF file (can be compressed with gz or zip). If no input is " +
						"specified, input will be read from standard input (stdin).",
				split = ",")
		private List<File> inputs;
		@Option(names = {"-o", "--output"},
				description = "Output file (only VCF, may be compressed). If no output is " +
						"specified, output will be written to standard output (stdout).")
		private File output;

		IOOptions() {
		}
	}

	@ArgGroup(validate = false,
			exclusive = false,
			heading = "@|bold Sample filtering and selecting options:|@%n")
	private SampleFiltering sf = new SampleFiltering();

	private static class SampleFiltering {
		@Option(names = {"--include-samples"},
				description = "List of samples to be included in the output. " +
						"Incompatible with --exclude-samples",
				arity = "0..*",
				split = ",")
		private List<String> includeSamples;

		@Option(names = {"--exclude-samples"},
				description = "List of samples to be excluded in the output. " +
						"Incompatible with --include-samples",
				arity = "0..*",
				split = ",")
		private List<String> excludeSamples;

		@Option(names = {"--homozygous"},
				description = "Filters the list of variants to include only those where these samples" +
						" are homozygous for the alternative allele.",
				arity = "0..*",
				split = ",")
		private List<String> homozygous;

		@Option(names = {"--heterozygous"},
				description = "Filters the list of variants to include only those where these samples" +
						" are heterozygous for the alternative allele.",
				arity = "0..*",
				split = ",")
		private List<String> heterozygous;

		@Option(names = {"--wildtype"},
				description = "Filters the list of variants to include only those where these samples" +
						" are wildtype for the alternative allele.",
				arity = "0..*",
				split = ",")
		private List<String> wildtype;
	}

	@ArgGroup(validate = false,
			exclusive = false,
			heading = "@|bold Variant filtering and selecting options:|@%n")
	private FilteringOptions fo = new FilteringOptions();

	private static class FilteringOptions {
		@Option(names = {"-f", "--filter"},
				arity = "0..*",
				description = "Adds a INFO field filter in the form: %n" +
						"\t[<sample>.]<key><operator><value>%n" +
						"@|bold sample|@ is optional and must be followed by a dot, the filter will" +
						"be applied to a FORMAT tag only for that sample, you can also leave it " +
						"blank (just type the dot) and it will be applied to all samples.%n" +
						"@|bold key|@ must be one of INFO ids%n" +
						"@|bold operator|@, a math (<, <=, >, >=, =) or string (=, !=, ?) " +
						"operator.%n" +
						"@|bold value|@ is the comparing value for the specified key, usually a" +
						" number or string. operator and value are optional for boolean keys.%n" +
						"If an INFO field is an array (A, G, R or .) all values must match the" +
						" filter, to make the filter pass in any of the values matches, then add" +
						" an * (asterisk) in front of the operator." +
						"@|underline Examples:|@%n" +
						"\t-f DP>5 (result will contain only variants which DP is greater than 5)%n" +
						"\t-f SIFTp?tolerated (filters variants which SIFT contains 'tolerated')%n" +
						"\t-f SYMBOL=LDLR (filter variants with SYMBOL = LDLR, if SYMBOL is an" +
						"array, all values must be LDLR)%n" +
						"\t-f EX_AF*<0.01 (filters variants with at least one ExAC frequency below" +
						" 0.01)%n" +
						"\t-f .DP>5 (filters variants where ALL samples have DP greater than 5)%n" +
						"\t-f NA001.GQ>=10 (sample NA001 must have GQ greater or equal to 10)")
		private List<String> patterns;
	}

	private final List<Filter> filters = new ArrayList<>();

	private PrintStream log = System.out;

	VariantFilter() {
	}

	@Override
	public Void call() throws Exception {
		final GenomicProgressBar bar = new GenomicProgressBar();
		if (io.output == null) log = System.err;
		if (sf.includeSamples != null && sf.excludeSamples != null)
			log.println("Incompatible options --include-samples and --exclude-samples");
		long line = 0;
		long passed = 0;
		// Use standard output if no output is provided
		final OutputStream out;
		if (io.output == null) out = System.out;
		else out = FileUtils.getOutputStream(io.output);

		//  Use standard input if no input is provided
		final List<InputStream> in = new ArrayList<>();
		if (io.inputs != null) {
			log.println(String.format("Detected %d input files: ", io.inputs.size()));
			for (File file : io.inputs) in.add(FileUtils.getInputStream(file));
		} else in.add(System.in);

		Variant variant = null;
		try (MultipleVariantReader reader = new MultipleVariantReader(in);
		     VariantWriter writer = new VariantWriter(out)) {
			// Create filters
			createFilters(reader.getHeader());
			setSamples(reader.getHeader());
			writer.setHeader(reader.getHeader());
			if (log == System.out) bar.start();
			final Iterator<Variant> iterator = reader.mergedIterator();
			while (iterator.hasNext()) {
				line += 1;
				variant = iterator.next();
				if (applyFilters(variant)) {
					writer.write(variant);
					passed += 1;
				}
				if (log == System.out)
					bar.update(variant.getCoordinate(), String.format("%s %,12d", variant.getCoordinate().getChrom(), variant.getCoordinate().getPosition()));
				if (line == 100) break;
			}
		} catch (Exception e) {
			throw new Exception(String.format("At line %d, variant %s ", line, variant), e);
		}
		if (log == System.out) bar.stop();
		log.printf("Read %d variants, %d passed%n", line, passed);
		return null;
	}

	private void setSamples(VcfHeader header) {
		if (sf == null) return;
		final int size = header.getSamples().size();
		if (sf.includeSamples != null)
			header.getSamples().retainAll(sf.includeSamples);
		else if (sf.excludeSamples != null)
			header.getSamples().removeAll(sf.excludeSamples);
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
		filters.add(new GenotypeFilter(header, gts));
		log.printf("Filtering genotypes:%n");
		gts.forEach((sample, set) -> log.printf(" - %s: %s%n", sample, set));
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

	private InfoFilter createFilter(String pattern, VcfHeader header) {
		int p = 0;
		String key;
		while (p < pattern.length() && Character.isLetterOrDigit(pattern.charAt(p))) p++;
		key = pattern.substring(0, p);
		if (!header.getInfoLines().containsKey(key)) return null;
		if (p == pattern.length())
			return new InfoFilter(key, Operator.EQ, true, true);
		Operator op = null;
		String value = null;
		boolean matchAll = true;
		int offset = 0;
		if (pattern.charAt(p) == '*') {
			matchAll = false;
			offset = 1;
		}
		for (Operator operator : Operator.values()) {
			final String symbol = operator.symbol;
			if (pattern.substring(p + offset, p + offset + symbol.length()).equals(symbol)) {
				op = operator;
				value = pattern.substring(p + offset + symbol.length());
				break;
			}
		}
		if (op == null) {
			log.printf("Unrecognized operator in filter %s%n", pattern);
			System.exit(1);
		}
		if (value.isBlank()) {
			log.printf("No value specified in filter %s%n", pattern);
			System.exit(1);
		}
		Object val = header.getInfoHeader(key).getProperty(value).getValue();
		if (val instanceof List) val = ((List) val).iterator().next();
		return new InfoFilter(key, op, val, matchAll);
	}

	private void createVariantFilters(VcfHeader header) {
		if (fo.patterns == null) return;
		for (String pattern : fo.patterns) {
			final InfoFilter filter = createFilter(pattern, header);
			if (filter != null) filters.add(filter);
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
		NEQ("!=", (a, b) -> !a.equals(b)),
		IN("?", (a, b) -> ((String) a).contains((String) b));

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
}
