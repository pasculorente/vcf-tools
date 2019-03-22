package org.uichuimi.vcf.utils;

import java.io.File;

import static picocli.CommandLine.Option;

public class AnnotatorCli {

//	@CommandLine.Option(names = {"-vcf"}, arity = "1..*", required = true)
//	private List<File> vcfs;
	@Option(names = {"-v", "--vcf"}, required = true)
	private File vcf;

	@Option(names = {"--vep"}, description = "VEP directory with homo_sapiens_incl_consequences-chr*.vcf.gz files", required = true)
	private File vep;

	@Option(names = {"-o", "--output"}, required = true)
	private File output;

	@Option(names = {"--1000G", "--1kG"}, required = true)
	private File _1000G;

	@Option(names = {"--genes"}, description = "path to GFF file (Homo_sapiens.GRCh38.95.gff3.gz)", required = true)
	private File genes;

	public File getVep() {
		return vep;
	}

	public File getOutput() {
		return output;
	}

	public File getVcf() {
		return vcf;
	}

	public File get1000G() {
		return _1000G;
	}

	public File getGenesPath() {
		return genes;
	}

	//	public List<File> getVcfs() {
//		return vcfs;
//	}
}
