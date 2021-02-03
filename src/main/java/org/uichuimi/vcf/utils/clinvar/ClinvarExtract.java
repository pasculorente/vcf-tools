package org.uichuimi.vcf.utils.clinvar;

import org.jetbrains.annotations.NotNull;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.FileUtils;
import org.uichuimi.vcf.utils.annotation.consumer.VariantConsumer;
import org.uichuimi.vcf.utils.exception.VcfException;
import org.uichuimi.vcf.variant.Variant;
import picocli.CommandLine.Model.CommandSpec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static picocli.CommandLine.*;

@Command(name = "clinvar-extract",
		version = "1.0",
		mixinStandardHelpOptions = true,
		description = "script that extracts diseases as a table from the file clinvar.vcf.gz")
public class ClinvarExtract implements Callable<Void> {

	@Spec
	private CommandSpec spec;

	@Parameters(description = "A vcf file (can be gzipped), with biallelic sites (only 1 alternate allele) and standard " +
			"clinvar annotations (CLNDISDB, CLNDN, CLNREVSTAT, CLNSIG, CLNSIGCONF). If not provided, standard " +
			"input is read", arity = "0..1")
	private File input;

	@Option(names = {"-o", "--variants"}, description = "If provided, a tsv file is created with columns:\n" +
			"[0] variant: variant id (chrom:pos:ref:alt)\n" +
			"[1] disease: disease id (database:identifier)\n" +
			"[2]     sig: (benign, conflicts, ...)\n" +
			"[3]    conf: conflicts explanation\n" +
			"[4]  status: revision status")
	private File variants;

	@Option(names = {"-d", "--diseases"}, description = "If provided, a tsv file is created with these columns:\n" +
			"[0]         id: clinvar id (database:identifier)\n" +
			"[1]   database: source database\n" +
			"[2] identifier: id in source database\n" +
			"[3]       name: disease name")
	private File diseases;

	@Option(names = {"-i", "--ignore"},
			description = "Do not export diseases (and related variants) with these identifiers.",
			showDefaultValue = Help.Visibility.ALWAYS, defaultValue = "MedGen:CN517202,MedGen:CN169374",
			split = ",", arity = "*")
	private List<String> ignore;

	@Option(names = {"-r", "--rs"}, description = "When specified, rs is taken from INFO/RS field, instead of ID. This" +
			"is where clinvar stores rs identifiers")
	private boolean rs;

	@Override
	public Void call() throws IOException {
		if (ignore == null) ignore = List.of();
		final List<VariantConsumer> consumers = new ArrayList<>();
		if (variants == null && diseases == null) {
			throw new ParameterException(spec.commandLine(), "At least one of -o or -d is required");
		}
		if (variants != null) consumers.add(new VariantExtractor(variants, ignore));
		if (diseases != null) consumers.add(new DiseasesExtractor(diseases, ignore));
		final InputStream in = input == null ? System.in : FileUtils.getInputStream(input);
		try (VariantReader reader = new VariantReader(in)) {
			for (VariantConsumer consumer : consumers) consumer.start(reader.getHeader());
			for (Variant variant : reader) {
				consumers.forEach(consumer -> consumer.accept(variant));
			}
			for (VariantConsumer consumer : consumers) consumer.close();
		} catch (IOException e) {
			usage(ClinvarExtract.class, System.out);
		} catch (Exception | VcfException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String rejoin(List<String> list) {
		return list == null || list.isEmpty() ? "" : String.join(",", list);
	}

	private List<List<String>> resplit(List<String> list) {
		if (list == null || list.isEmpty()) return List.of();
		return Arrays.stream(String.join(",", list).split("\\|"))
				.map(s -> s.equals(".") ? List.<String>of() : List.of(s.split(",")))
				.collect(Collectors.toList());
	}


	private class VariantExtractor implements VariantConsumer {

		private final PrintStream out;
		private final Set<String> ignored;
		private Function<Variant, String> idExtractor;

		public VariantExtractor(File output, List<String> ignored) throws IOException {
			out = new PrintStream(FileUtils.getOutputStream(output));
			this.ignored = Set.copyOf(ignored);
			out.println(String.join("\t", ":START_ID(variant)", ":END_ID(disease)", "significance", "conflicts", "status"));
		}

		@Override
		public void start(VcfHeader header) throws VcfException {
			idExtractor = rs ? getRsExtractor(header)
					: variant -> String.format("%s:%d:%s:%s",
					variant.getCoordinate().getChrom(),
					variant.getCoordinate().getPosition(),
					variant.getReferences().get(0),
					variant.getAlternatives().get(0));
		}

		@NotNull
		private Function<Variant, String> getRsExtractor(VcfHeader header) {
			return header.hasComplexHeader("INFO", "RS")
					? (variant -> variant.getInfo().contains("RS")
					? ((List<String>) variant.getInfo("RS")).stream()
					.map(rsid -> "rs" + rsid)
					.collect(Collectors.joining(","))
					: ".")
					: (variant -> String.join(",", variant.getIdentifiers()));
		}

		@Override
		public void accept(Variant variant) {
			if (variant.getAlternatives().isEmpty()) return;
			final List<List<String>> clndisdb = resplit(variant.getInfo("CLNDISDB"));
			if (clndisdb.isEmpty()) return;
			// Sig, revstat and conf are always size 1
			final String clnsig = rejoin(variant.getInfo("CLNSIG")).replace("_", " ");
			final String clnsigconf = rejoin(variant.getInfo("CLNSIGCONF")).replace("_", " ");
			final String clnrevstat = rejoin(variant.getInfo("CLNREVSTAT")).replace("_", " ");
			final String id = idExtractor.apply(variant);
			for (final List<String> identifiers : clndisdb) {
				for (String identifier : identifiers) {
					if (!ignored.contains(identifier))
						out.println(String.join("\t", id, identifier,
								clnsig.isEmpty() ? "." : clnsig,
								clnsigconf.isEmpty() ? "." : clnsigconf,
								clnrevstat.isEmpty() ? "." : clnrevstat));
				}
			}
		}

		@Override
		public void close() {
			out.close();
		}
	}

	private class DiseasesExtractor implements VariantConsumer {
		final PrintStream out;
		final Set<String> exported = new TreeSet<>();
		final Set<String> ignored;

		public DiseasesExtractor(File output, List<String> ignore) throws IOException {
			out = new PrintStream(FileUtils.getOutputStream(output));
			ignored = Set.copyOf(ignore);
			out.println(String.join("\t", ":ID(disease)", "database", "identifier", "name"));
		}

		@Override
		public void start(VcfHeader header) throws VcfException {

		}

		@Override
		public void accept(Variant variant) {
			final List<List<String>> clndisdb = resplit(variant.getInfo("CLNDISDB"));
			if (clndisdb.isEmpty()) return;
			final List<List<String>> clndn = resplit(variant.getInfo("CLNDN"));
			for (int i = 0; i < clndisdb.size(); i++) {
				final List<String> identifiers = clndisdb.get(i);
				final List<String> names = clndn.get(i);
				final String name = String.join(",", names).replace("_", " ");
				for (String identifier : identifiers) {
					if (!ignored.contains(identifier) && !exported.contains(identifier)) {
						exported.add(identifier);
						final String[] split = identifier.split(":", 2);
						final String src = split[0];
						final String id = split[1];
						out.println(String.join("\t", identifier, src, id, name));
					}
				}
			}
		}

		@Override
		public void close() {
			out.close();
		}
	}
}
