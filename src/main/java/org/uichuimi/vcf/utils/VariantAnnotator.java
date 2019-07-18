package org.uichuimi.vcf.utils;

import org.uichuimi.vcf.header.SimpleHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.MultipleVariantReader;
import org.uichuimi.vcf.utils.common.CoordinateUtils;
import org.uichuimi.vcf.utils.common.GenomeProgress;
import org.uichuimi.vcf.utils.common.ProgressBar;
import org.uichuimi.vcf.utils.consumer.*;
import org.uichuimi.vcf.utils.consumer.neo4j.Neo4jTablesWriter;
import org.uichuimi.vcf.utils.consumer.vep.VepAnnotator;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

/**
 * Allows the annotation of VCF files with VEP and 1000Genomes. Input must be 1 or more VCF files,
 * Output to a VCF file or neo4j tables (3rd normal form). It is encouraged to used the Ensembl
 * reference genome (GRCh38), although UCSC genome should work as well.
 */
@Command(name = "annotate", description = "annotates vcf files using VEP, 1kg and other resources")
class VariantAnnotator implements Callable<Void> {

	@Option(names = {"--input", "-i"},
			arity = "1..*",
			description = "Input VCF files (can be compressed with gz or zip)",
			required = true)
	private List<File> inputs;

	@Option(names = {"--vep"},
			description = "VEP directory with homo_sapiens_incl_consequences-chr*.vcf.gz files")
	private File vep;

	@Option(names = {"--1000G", "--1kG"},
			description = "1000 genomes phase 3 VCF with population frequencies (1000GENOMES-phase_3.vcf.gz)")
	private File kGenomes;

	@Option(names = {"--gff"},
			description = "GFF file with Ensembl genes (Homo_sapiens.GRCh38.95.gff3.gz)")
	private File genes;

	@Option(names = {"-o", "--output"},
			description = "output file (VCF format)")
	private File output;

	@Option(names = {"--neo4j"},
			description = "Path to output folder of neo4j CSV files")
	private File neo4j;

	@Option(names = {"--gnomadGenomes", "--gnomad-genomes"},
			description = "File with gnomAD genomes frequencies (AF_[afr|amr|eas|nfe|fin|asj|oth]). If it is a directory, then files must have the format gnomad.genomes.chr{}.vcf.gz")
	private File gnomadGenomes;

	@Option(names = {"--gnomadExomes", "--gnomad-exomes"},
			description = "File with gnomAD exomes frequencies (AF_[afr|amr|asj|eas|fin|nfe|oth|sas]). If it is a directory, then files must have the format gnomad.exomes.chr{}.vcf.gz")
	private File gnomadExomes;

	@Option(names = {"--exac", "--ExAC"},
			description = "File with ExAC frequencies (AC_[AFR|AMR|EAS|FIN|NFE|OTH|SAS] + AN_Adj) from Ensembl (ExAC.0.3.GRCh38.vcf.gz)")
	private File exac;

	@Option(names = {"--compute-stats"}, description = "Whether to compute DP, AN, AC and AF again.")
	private boolean compute;

	public VariantAnnotator() {
	}

	public VariantAnnotator(List<File> inputs, File output) {
		this.inputs = inputs;
		this.output = output;
	}

	// Builder pattern
	public VariantAnnotator setVep(File vep) {
		this.vep = vep;
		return this;
	}

	public VariantAnnotator setkGenomes(File kGenomes) {
		this.kGenomes = kGenomes;
		return this;
	}

	public VariantAnnotator setGenes(File genes) {
		this.genes = genes;
		return this;
	}

	public VariantAnnotator setNeo4j(File neo4j) {
		this.neo4j = neo4j;
		return this;
	}

	public VariantAnnotator setGnomadGenomes(File gnomadGenomes) {
		this.gnomadGenomes = gnomadGenomes;
		return this;
	}

	public VariantAnnotator setGnomadExomes(File gnomadExomes) {
		this.gnomadExomes = gnomadExomes;
		return this;
	}

	public VariantAnnotator setExac(File exac) {
		this.exac = exac;
		return this;
	}

	public VariantAnnotator setCompute(boolean compute) {
		this.compute = compute;
		return this;
	}

	@Override
	public Void call() throws Exception {

		final List<VariantConsumer> consumers = new ArrayList<>();
		final ProgressBar bar = new ProgressBar();
		int c = 0;
		Variant variant = null;
		try (final MultipleVariantReader variantReader = MultipleVariantReader.getInstance(inputs)) {
			final List<String> samples = variantReader.getHeader().getSamples().stream().sorted().distinct().collect(Collectors.toList());
			System.out.printf("Found %d samples (%s)%n", samples.size(), String.join(", ", samples));
			// Create consumers depending on the options
			System.out.println("Adding consumers:");
			writeCommandLine(variantReader.getHeader());
			// 1. Additive consumers
			if (kGenomes != null) {
				System.out.println(" - 1000G frequencies from " + kGenomes);
				consumers.add(new KGenomesAnnotator(kGenomes));
			}
			if (vep != null && genes != null) {
				System.out.println(" - Variant effect predictions from " + vep);
				System.out.println("    - Genes are taken from " + genes);
				consumers.add(new VepAnnotator(genes, vep));
			}
			if (gnomadGenomes != null) {
				System.out.println(" - Adding gnomAD genomes frequencies from " + gnomadGenomes);
				consumers.add(new GnomadGenomeAnnotator(gnomadGenomes));
			}
			if (gnomadExomes != null) {
				System.out.println(" - Adding gnomAD exomes frequencies from " + gnomadExomes);
				consumers.add(new GnomadExomeAnnotator(gnomadExomes));
			}
			if (exac != null) {
				System.out.println(" - Adding ExAC frequencies from " + exac);
				consumers.add(new ExACAnnotator(exac));
			}
			// 2. Modifier consumers
			if (compute) {
				System.out.println(" - DP and AN (global) and AF and AC (per allele) will be recomputed");
				consumers.add(new StatsCalculator());
			}
			// 3. Output consumers
			if (neo4j != null) {
				System.out.println(" - Tables for neo4j into " + neo4j);
				consumers.add(new Neo4jTablesWriter(neo4j));
			}
			if (output != null) {
				System.out.println(" - Export VCF into " + output);
				consumers.add(new VcfWriter(output));
			}

			// Initialize consumers
			bar.start();
			bar.update(0.0, "Initializing...");
			consumers.forEach(consumer -> consumer.start(variantReader.getHeader()));

			// Iterate over input
			while (variantReader.hasNext()) {
				variant = variantReader.nextMerged();
				final Coordinate coordinate = variant.getCoordinate();
				final Coordinate grch38 = CoordinateUtils.toGrch38(coordinate);

				// Apply every consumer
				for (VariantConsumer consumer : consumers)
					consumer.accept(variant, grch38);

				if (++c % 1000 == 0) {
					final double progress = GenomeProgress.getProgress(grch38);
					final long l = TimeUnit.NANOSECONDS.toSeconds(bar.getElapsedNanos());
					final long sitesPerSecond = c / (1 + l); // avoid division by 0
					final String message = String.format("%,d (%d sites/sec) %s %,d", c, sitesPerSecond, coordinate.getChrom(), coordinate.getPosition());
					bar.update(progress, message);
				}
			}

		} catch (Exception e) {
			throw new Exception(String.format("At line %d, variant %s", c, variant), e);
		} finally {
			// Close consumers
			bar.update(0.99, "Closing consumers...");
			consumers.forEach(VariantConsumer::close);

		}
		bar.update(1.0, "Completed");
		bar.stop();
		return null;
	}

	private void writeCommandLine(VcfHeader header) {
		header.addHeaderLine(new SimpleHeaderLine("CommandLine", getCommandLine()));
	}

	private String getCommandLine() {
		final StringBuilder builder = new StringBuilder("vcf-tools annotate");
		for (File input : inputs) builder.append(" --input ").append(input);
		if (vep != null) builder.append(" --vep ").append(vep);
		if (kGenomes != null) builder.append(" --1000G ").append(kGenomes);
		if (genes != null) builder.append(" --gff ").append(genes);
		if (output != null) builder.append(" --output ").append(output);
		if (neo4j != null) builder.append(" --neo4j ").append(neo4j);
		if (gnomadExomes != null) builder.append(" --gnomadExomes ").append(gnomadExomes);
		if (gnomadGenomes != null) builder.append(" --gnomadGenomes ").append(gnomadGenomes);
		if (exac != null) builder.append(" --exac ").append(exac);
		if (compute) builder.append(" --compute-stats");
		return builder.toString();
	}

}
