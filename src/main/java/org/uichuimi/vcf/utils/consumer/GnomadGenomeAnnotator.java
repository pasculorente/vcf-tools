package org.uichuimi.vcf.utils.consumer;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class GnomadGenomeAnnotator extends FrequencyAnnotator {

	private final static String fileName = "gnomad.genomes.chr%s.vcf.gz";

	/**
	 * @param source a single vcf file, or a directory with one vcf per chromosome.
	 */
	public GnomadGenomeAnnotator(File source) {
		super(source);
	}

	@Override
	protected Collection<FrequencyAnnotation> getAnnotations() {
		return List.of(
				new FrequencyAnnotation("AF_afr", "GG_AFR_AF", "Allele frequency in African population (gnomAD genomes)"),
				new FrequencyAnnotation("AF_amr", "GG_AMR_AF", "Allele frequency in American population (gnomAD genomes)"),
				new FrequencyAnnotation("AF_eas", "GG_EAS_AF", "Allele frequency in East Asian population (gnomAD genomes)"),
				new FrequencyAnnotation("AF_nfe", "GG_NFE_AF", "Allele frequency in European non Finnish population (gnomAD genomes)"),
				new FrequencyAnnotation("AF_fin", "GG_FIN_AF", "Allele frequency in Finnish population (gnomAD genomes)"),
				new FrequencyAnnotation("AF_oth", "GG_OTH_AF", "Allele frequency in other populations (gnomAD genomes)"),
				new FrequencyAnnotation("AF_asj", "GG_ASJ_AF", "Allele frequency in Ashkenazi Jew population (gnomAD genomes)"));
	}

	@Override
	protected String getFileName(String chrom) {
		return String.format(fileName, chrom);
	}
}
