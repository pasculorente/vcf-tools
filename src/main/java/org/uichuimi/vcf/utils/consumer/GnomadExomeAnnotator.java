package org.uichuimi.vcf.utils.consumer;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class GnomadExomeAnnotator extends FrequencyAnnotator {

	private final static String fileName = "gnomad.exomes.%s.vcf.gz";

	/**
	 * @param gnomad a single vcf file, or a directory with one vcf per chromosome.
	 */
	public GnomadExomeAnnotator(File gnomad) {
		super(gnomad);
	}

	@Override
	protected Collection<FrequencyAnnotation> getAnnotations() {
		return List.of(
				new FrequencyAnnotation("AF_afr", "GE_AFR_AF", "Allele frequency in African population (gnomAD exomes)"),
				new FrequencyAnnotation("AF_amr", "GE_AMR_AF", "Allele frequency in America population (gnomAD exomes)"),
				new FrequencyAnnotation("AF_asj", "GE_ASJ_AF", "Allele frequency in Ashkenazi Jew population (gnomAD exomes)"),
				new FrequencyAnnotation("AF_eas", "GE_EAS_AF", "Allele frequency in East Asian population (gnomAD exomes)"),
				new FrequencyAnnotation("AF_fin", "GE_FIN_AF", "Allele frequency in Finnish population (gnomAD exomes)"),
				new FrequencyAnnotation("AF_nfe", "GE_NFE_AF", "Allele frequency in European non Finnish population (gnomAD exomes)"),
				new FrequencyAnnotation("AF_oth", "GE_OTH_AF", "Allele frequency in Other population (gnomAD exomes)"),
				new FrequencyAnnotation("AF_sas", "GE_SAS_AF", "Allele frequency in South Asian population (gnomAD exomes)")
				);
	}

	@Override
	protected String getFileName(String chrom) {
		return String.format(fileName, chrom);
	}
}
