package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.MultipleVariantReader;
import org.uichuimi.vcf.io.VariantWriter;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.utils.common.GenomicProgressBar;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

/**
 * Allows filtering vcf files.
 */
@Command(name = "filter", description = "filters vcf files")
public class VariantFilter implements Callable<Void> {

	@Option(names = {"--input", "-i"},
			description = "Input VCF file (can be compressed with gz or zip)",
			split = ",",
			required = true)
	private List<File> inputs;

	@Option(names = {"-o", "--output"},
			description = "output file (VCF format)")
	private File output;

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

	private PrintStream log = System.out;

	@Override
	public Void call() throws Exception {
		GenomicProgressBar bar = new GenomicProgressBar();
		if (output == null) log = System.err;
		if (includeSamples != null && excludeSamples != null)
			log.println("Incompatible options --include-samples and --exclude-samples");
		long line = 0;
		// Use standard output if no output is provided
		final OutputStream out;
		if (output == null) out = System.out;
		else out = FileUtils.getOutputStream(output);

		//  Use standard input if no input is provided
		final List<InputStream> in = new ArrayList<>();
		if (inputs != null) {
			log.println(String.format("Detected %d input files: ", inputs.size()));
			for (File file : inputs) in.add(FileUtils.getInputStream(file));
		} else in.add(System.in);

		Variant variant = null;
		try (MultipleVariantReader reader = new MultipleVariantReader(in);
		     VariantWriter writer = new VariantWriter(out)) {
			setSamples(reader.getHeader());
			writer.setHeader(reader.getHeader());
			if (log == System.out) bar.start();
			final Iterator<Variant> iterator = reader.mergedIterator();
			while (iterator.hasNext()) {
				line += 1;
				variant = iterator.next();
				applyFilters();
				writer.write(variant);
				if (log == System.out)
					bar.update(variant.getCoordinate(), String.format("%s %,12d", variant.getCoordinate().getChrom(), variant.getCoordinate().getPosition()));
			}
		} catch (Exception e) {
			throw new Exception(String.format("At line %d, variant %s ", line, variant), e);
		}
		return null;
	}

	private void setSamples(VcfHeader header) {
		final int size = header.getSamples().size();
		if (includeSamples != null)
			header.getSamples().retainAll(includeSamples);
		else if (excludeSamples != null)
			header.getSamples().removeAll(excludeSamples);
		log.println(String.format("Found %d samples. Sending to output %d: %s",
				size, header.getSamples().size(), header.getSamples()));
	}

	private void applyFilters() {

	}
}
