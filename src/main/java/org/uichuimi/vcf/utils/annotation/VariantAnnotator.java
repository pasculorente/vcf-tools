package org.uichuimi.vcf.utils.annotation;

import org.uichuimi.vcf.header.SimpleHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.MultipleVariantReader;
import org.uichuimi.vcf.utils.annotation.consumer.*;
import org.uichuimi.vcf.utils.annotation.consumer.dbsnp.DbsnpAnnotator;
import org.uichuimi.vcf.utils.annotation.consumer.neo4j.Neo4jTablesWriter;
import org.uichuimi.vcf.utils.annotation.consumer.snpeff.SnpEffExtractor;
import org.uichuimi.vcf.utils.annotation.consumer.vep.VepAnnotator;
import org.uichuimi.vcf.utils.annotation.gff.GeneMap;
import org.uichuimi.vcf.utils.common.GenomeProgress;
import org.uichuimi.vcf.utils.common.GenomicProgressBar;
import org.uichuimi.vcf.variant.Chromosome;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
public
class VariantAnnotator implements Callable<Void> {

	@Option(names = {"--input", "-i"},
			arity = "1..*",
			description = "Input VCF files (can be compressed with gz or zip)",
			required = true)
	private List<File> inputs;

	@Option(names = "--namespace",
			description = "Namespace for contigs in inputs (GRCH, UCSC, REFSEQ, GENEBANK)",
			defaultValue = "GRCH")
	private Chromosome.Namespace namespace;

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

	@Option(names = {"--snpeff"}, description = "Whether the input file or stream contains snpeff ANN. In this case extract consequence info from this INFO.")
	private Boolean snpeff;

	@Option(names = {"--dbsnp"}, description = "Dbsnp file from NCBI (ftp://ftp.ncbi.nih.gov/snp/latest_release/VCF/GCF_000001405.38.gz)")
	private File dbsnp;

	@Option(names = {"-a", "--annotations"}, description = "VCF file with annotations to add FORMAT -> filename=COL1,COL2 or filename=COL1:ALIAS,COL2")
	private List<String> annotations;

	private GeneMap geneMap;
	private OutputStream outputStream;

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

	public VariantAnnotator setNamespace(Chromosome.Namespace namespace) {
		this.namespace = namespace;
		return this;
	}

	public VariantAnnotator setDbsnp(File dbsnp) {
		this.dbsnp = dbsnp;
		return this;
	}

	public VariantAnnotator setSnpeff(Boolean snpeff) {
		this.snpeff = snpeff;
		return this;
	}

	public VariantAnnotator setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
		return this;
	}

	@Override
	public Void call() throws Exception {
		Locale.setDefault(Locale.ENGLISH);
		final PrintStream log;
		final boolean showProgress;
		if (output != null) {
			log = System.out;
			showProgress = true;
		} else if (outputStream != null && outputStream != System.out) {
			log = System.out;
			showProgress = true;
		} else {
			log = System.err;
			showProgress = false;
		}
		final List<VariantConsumer> consumers = new ArrayList<>();
		final GenomicProgressBar bar = new GenomicProgressBar(log);
		int line = 0;
		Variant variant = null;
		try (final MultipleVariantReader reader = MultipleVariantReader.getInstance(inputs, namespace)) {
			final List<String> samples = reader.getHeader().getSamples().stream().sorted().distinct().collect(Collectors.toList());
			log.printf("Found %d samples (%s)%n", samples.size(), String.join(", ", samples));
			// Create consumers depending on the options
			log.println("Adding consumers:");
			writeCommandLine(reader.getHeader());
			// 1. Additive consumers
			if (kGenomes != null) {
				log.println(" - 1000G frequencies from " + kGenomes);
				consumers.add(new KGenomesAnnotator(kGenomes));
			}
			if (genes != null) {
				log.println(" - Reading genes from " + genes);
				geneMap = new GeneMap(genes);
			}
			if (vep != null && genes != null) {
				log.println(" - Variant effect predictions from " + vep);
				consumers.add(new VepAnnotator(vep, geneMap));
			}
			if (snpeff != null && snpeff) {
				log.println(" - Extracting consequences from ANN tag");
				consumers.add(new SnpEffExtractor(geneMap));
			}
			if (gnomadGenomes != null) {
				log.println(" - Adding gnomAD genomes frequencies from " + gnomadGenomes);
				consumers.add(new GnomadGenomeAnnotator(gnomadGenomes));
			}
			if (gnomadExomes != null) {
				log.println(" - Adding gnomAD exomes frequencies from " + gnomadExomes);
				consumers.add(new GnomadExomeAnnotator(gnomadExomes));
			}
			if (exac != null) {
				log.println(" - Adding ExAC frequencies from " + exac);
				consumers.add(new ExACAnnotator(exac));
			}
			if (dbsnp != null) {
				log.printf(" - Adding rs identifier from dbSNP (%s)%n", dbsnp);
				consumers.add(new DbsnpAnnotator(dbsnp));
			}
			if (annotations != null) {
				for (String annotation : annotations) {
					final String[] split = annotation.split("=");
					final String filename = split[0];
					final String columnSpec = split[1];
					final File file = new File(filename);
					final List<ColumnSpec> columnSpecs = new ArrayList<>();
					for (String colSpec : columnSpec.split(",")) {
						final String[] split1 = colSpec.split(":");
						final String srcCol = split1[0];
						final String tgtCol = split1.length > 1 ? split1[1] : srcCol;
						columnSpecs.add(new ColumnSpec(srcCol, tgtCol));
					}
					final VcfAnnotator vcfAnnotator = new VcfAnnotator(file, columnSpecs);
					final String cols = vcfAnnotator.getColumnSpecs().stream().map(ColumnSpec::getSourceColumn).collect(Collectors.joining(", "));
					log.println(" - Add annotations [" + cols + "] from " + filename);
					consumers.add(vcfAnnotator);
				}

			}
			// 2. Modifier consumers
			if (compute) {
				log.println(" - DP and AN (global) and AF and AC (per allele) will be recomputed");
				consumers.add(new StatsCalculator());
			}
			// 3. Output consumers
			if (neo4j != null) {
				log.println(" - Tables for neo4j into " + neo4j);
				consumers.add(new Neo4jTablesWriter(neo4j));
			}
			if (output != null) {
				log.println(" - Export VCF into " + output);
				consumers.add(new VcfWriter(output, namespace));
			} else if (outputStream != null) {
				log.println(" - Export VCF to custom output stream");
				consumers.add(new VcfWriter(outputStream, namespace));
			} else {
				// No output specified, default to System.out, only if no other output option is specified, like neo4j
				if (neo4j == null) {
					log.println(" - Export VCF to standard output");
					consumers.add(new VcfWriter(System.out, namespace));
				}
			}

			// Initialize consumers
			if (showProgress) {
				bar.start();
				bar.update(0.0, "Initializing...");
			}
			consumers.forEach(consumer -> consumer.start(reader.getHeader()));

			// Iterate over input
			while (reader.hasNext()) {
				variant = reader.nextMerged();
				final Coordinate coordinate = variant.getCoordinate();

				// Apply every consumer
				for (VariantConsumer consumer : consumers)
					consumer.accept(variant);

				if (++line % 1000 == 0 && showProgress) {
					final double progress = GenomeProgress.getProgress(variant.getCoordinate());
					final long elapsed = TimeUnit.NANOSECONDS.toSeconds(bar.getElapsedNanos());
					final long sitesPerSecond = line / (1 + elapsed); // avoid division by 0
					final String message = String.format("%,d (%d sites/sec) %s %,d", line, sitesPerSecond, coordinate.getChrom(), coordinate.getPosition());
					bar.update(progress, message);
				}
			}
		} catch (InterruptedException e) {
			log.println("User canceled");
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			throw new Exception(String.format("At line %d, variant %s", line, variant), e);
		} finally {
			// Close consumers
			if (showProgress) bar.update(0.99, "Closing consumers...");
			consumers.forEach(VariantConsumer::close);

		}
		if (showProgress) {
			bar.update(1.0, "Completed");
			bar.stop();
			final long elapsed = TimeUnit.NANOSECONDS.toSeconds(bar.getElapsedNanos());
			final long sitesPerSecond = line / (1 + elapsed); // avoid division by 0
			log.printf("%,d sites processed (%,d sites per second)%n", line, sitesPerSecond);
		}
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
		if (snpeff != null) builder.append(" --snpeff");
		if (dbsnp != null) builder.append(" --dbsnp ").append(dbsnp);
		if (annotations != null)
			for (String annotation : annotations)
			builder.append(" --annotations ").append(annotation);

		return builder.toString();
	}

}
