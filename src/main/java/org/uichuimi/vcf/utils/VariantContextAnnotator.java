package org.uichuimi.vcf.utils;

import org.uichuimi.vcf.input.MultipleVariantContextReader;
import org.uichuimi.vcf.utils.common.CoordinateUtils;
import org.uichuimi.vcf.utils.common.GenomeProgress;
import org.uichuimi.vcf.utils.common.ProgressBar;
import org.uichuimi.vcf.utils.consumer.*;
import org.uichuimi.vcf.utils.consumer.neo4j.Neo4jTablesWriter;
import org.uichuimi.vcf.utils.consumer.vep.VepAnnotator;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.VariantContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

/**
 * Allows the annotation of VCF files with VEP and 1000Genomes. Input must be 1 or more VCF files, Output to a VCF file
 * or neo4j tables (3rd normal form). It is encouraged to used the Ensembl reference genome (GRCh38), although UCSC
 * genome should work as well.
 */
@Command(name = "annotate", description = "annotates vcf files using VEP, 1kg and other resources")
class VariantContextAnnotator implements Callable<Void> {

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

	@Option(names = {"--compute-stats"}, description = "Whether to compute DP, AN, AC and AF again.")
	private boolean compute;

	@Override
	public Void call() throws Exception {

		final List<VariantConsumer> consumers = new ArrayList<>();
		final ProgressBar bar = new ProgressBar();
		int c = 0;
		try (final MultipleVariantContextReader variantReader = MultipleVariantContextReader.getInstance(inputs)) {
			final List<String> samples = variantReader.getHeader().getSamples().stream().sorted().distinct().collect(Collectors.toList());
			System.out.printf("Found %d samples (%s)%n", samples.size(), String.join(", ", samples));
			// Create consumers depending on the options
			// 1. Additive consumers
			System.out.println("Adding consumers:");
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
				final VariantContext variant = variantReader.nextMerged();
				final Coordinate coordinate = variant.getCoordinate();
				final Coordinate grch38 = CoordinateUtils.toGrch38(coordinate);

				// Apply every consumer
				for (VariantConsumer consumer : consumers)
					consumer.accept(variant, grch38);

				if (++c % 1000 == 0) {
					final double progress = GenomeProgress.getProgress(grch38);
					final String message = String.format("%,d %s %,d", c, coordinate.getChrom(), coordinate.getPosition());
					bar.update(progress, message);
				}
			}

			// Close consumers
			bar.update(0.99, "Closing consumers...");
		} catch (Exception e) {
			throw new Exception("At line " + c, e);
		} finally {
			consumers.forEach(VariantConsumer::close);

		}
		bar.update(1.0, "Completed");
		bar.stop();
		return null;
	}

}
