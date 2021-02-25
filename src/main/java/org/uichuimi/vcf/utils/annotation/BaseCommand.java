package org.uichuimi.vcf.utils.annotation;


import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.MultipleVariantReader;
import org.uichuimi.vcf.io.VariantInputReader;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.io.VariantWriter;
import org.uichuimi.vcf.utils.common.GenomeProgress;
import org.uichuimi.vcf.utils.common.GenomicProgressBar;
import org.uichuimi.vcf.variant.Chromosome;
import org.uichuimi.vcf.variant.Chromosome.Namespace;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static picocli.CommandLine.Option;

public abstract class BaseCommand implements Callable<Void> {

	@Option(names = {"--input", "-i"},
			arity = "1..*",
			description = "Input VCF files (can be compressed with gz or zip)")
	private List<File> inputs;

	@Option(names = {"--namespace", "-N"},
			description = "Namespace for contigs in inputs (GRCH, UCSC, REFSEQ, GENEBANK)",
			defaultValue = "GRCH")
	private Chromosome.Namespace namespace;

	@Option(names = {"--output-namespace", "--ons"},
			description = "Namespace for contigs in inputs (GRCH, UCSC, REFSEQ, GENEBANK)",
			defaultValue = "GRCH")
	private Chromosome.Namespace outputNamespace;

	@Option(names = {"-o", "--output"},
			description = "output file (VCF format)")
	private File output;

	@Option(names = {"--progress"}, description = "Shows progress bar", defaultValue = "true")
	private boolean progress;

	@Override
	public Void call() throws Exception {
		Locale.setDefault(Locale.ENGLISH);
		// Log is done to stdout if an output file is specified
		// or to stderr if output goes to stdout
		final PrintStream log;
		if (output != null) {
			log = System.out;
		} else {
			log = System.err;
		}
		final GenomicProgressBar bar = new GenomicProgressBar(log);

		// Determine input
		VariantInputReader reader;
		if (inputs == null) {
			reader = new VariantReader(System.in, namespace);
		} else if (inputs.size() == 1) {
			reader = new VariantReader(inputs.get(0));
		} else {
			reader = MultipleVariantReader.getInstance(inputs, namespace);
		}

		// Determine output
		VariantWriter writer;
		Namespace ns = outputNamespace == null ? reader.getHeader().getNamespace() : outputNamespace;
		if (output == null) {
			writer = new VariantWriter(System.out, ns);
		} else {
			writer = new VariantWriter(output, ns);
		}

		try (reader; writer) {
			// Initialization
			if (progress) {
				bar.start();
				bar.update(0.0, "Initializing...");
			}

			setUp(reader.getHeader());
			writer.setHeader(reader.getHeader());

			// Iterate over input
			Variant variant;
			long line = 0;
			while (reader.hasNext()) {
				variant = reader.next();
				consume(variant, line);
				writer.write(variant);

				if (progress && ++line % 1000 == 0) {
					final Coordinate coordinate = variant.getCoordinate();
					final double progress = GenomeProgress.getProgress(coordinate);
					final long elapsed = TimeUnit.NANOSECONDS.toSeconds(bar.getElapsedNanos());
					final long sitesPerSecond = line / (1 + elapsed); // avoid division by 0
					final String message = String.format("%,d (%d sites/sec) %s %,d", line, sitesPerSecond, coordinate.getChrom(), coordinate.getPosition());
					bar.update(progress, message);
				}
			}
			tearDown();
			if (progress) {
				bar.update(1.0, "Completed");
				bar.stop();
				final long elapsed = TimeUnit.NANOSECONDS.toSeconds(bar.getElapsedNanos());
				final long sitesPerSecond = line / (1 + elapsed); // avoid division by 0
				log.printf("%,d sites processed (%,d sites per second)%n", line, sitesPerSecond);
			}
		}
		return null;
	}

	protected abstract void setUp(VcfHeader header);

	protected abstract void consume(Variant variant, long line);

	protected abstract void tearDown();
}
