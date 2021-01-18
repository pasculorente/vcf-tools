package org.uichuimi.vcf.utils.dbsnp;

import org.uichuimi.vcf.header.SimpleHeaderLine;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.io.VariantWriter;
import org.uichuimi.vcf.utils.FileUtils;
import org.uichuimi.vcf.utils.common.GenomeProgress;
import org.uichuimi.vcf.utils.common.GenomicProgressBar;
import org.uichuimi.vcf.utils.exception.VcfException;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(name = "dbnsfp")
public class DbnsfpCommand implements Callable<Void> {

	@Option(names = {"-t", "--transcript-policy"}, defaultValue = "all",
			description = "How to treat annotations at transcript level. Must be one of all|first|tag. " +
					"If first is selected, the first value of each tag is taken. If all is selected, then all values" +
					"are taken. If tag is given, then the INFO field 'tag' is used to determine which element of the " +
					"array to take.")
	private String transcriptPolicy;

	@Option(names = {"-d", "--dbnsfp"}, description = "dbNSFP file", required = true)
	private File dbnsfp;

	@Option(names = {"-f", "--fields"}, split = ",")
	private List<String> fields;

	@Option(names = {"-o", "--output"},
			description = "output file (VCF format)")
	private File output;

	@Option(names = {"--input", "-i"},
			description = "Input VCF files (can be compressed with gz or zip).")
	private File input;


	@Override
	public Void call() throws Exception {
		Locale.setDefault(Locale.ENGLISH);
		final StringBuilder command = new StringBuilder("vcf-utils dbsnp")
				.append(" -f ").append(String.join(",", fields))
				.append(" -t ").append(transcriptPolicy);
		if (input != null) command.append(" -i ").append(input);
		if (output != null) command.append(" -o ").append(output);
		final OutputStream outputStream = output == null ? System.out : FileUtils.getOutputStream(output);
		final InputStream inputStream = input == null ? System.in : FileUtils.getInputStream(input);
		final DbNSPFPAnnotator annotator = new DbNSPFPAnnotator(dbnsfp, fields, transcriptPolicy);
		final boolean showProgress = output != null;
		GenomicProgressBar bar = new GenomicProgressBar(System.out);
		Variant variant;
		try (final VariantReader reader = new VariantReader(inputStream);
		     final VariantWriter writer = new VariantWriter(outputStream, reader.getHeader().getNamespace())) {

			// Initialize consumers
			if (showProgress) {
				bar.start();
				bar.update(0.0, "Initializing...");
			}

			annotator.start(reader.getHeader());
			reader.getHeader().addHeaderLine(new SimpleHeaderLine("command", command.toString()));
			writer.setHeader(reader.getHeader());

			long line = 0;

			while (reader.hasNext()) {
				variant = reader.next();
				annotator.accept(variant);
				writer.write(variant);
				if (++line % 1000 == 0 && showProgress) {
					final Coordinate coordinate = variant.getCoordinate();
					final double progress = GenomeProgress.getProgress(coordinate);
					final long elapsed = TimeUnit.NANOSECONDS.toSeconds(bar.getElapsedNanos());
					final long sitesPerSecond = line / (1 + elapsed); // avoid division by 0
					final String message = String.format("%,d (%d sites/sec) %s %,d", line, sitesPerSecond, coordinate.getChrom(), coordinate.getPosition());
					bar.update(progress, message);
				}

			}
		} catch (VcfException e) {
			e.printStackTrace();
		}

		return null;
	}
}
