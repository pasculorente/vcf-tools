package org.uichuimi.vcf.utils.consumer;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class KGenomesAnnotator extends FrequencyAnnotator {

	public KGenomesAnnotator(File file) {
		super(file);
	}

	@Override
	protected Collection<FrequencyAnnotation> getAnnotations() {
		return List.of(
				new FrequencyAnnotation("AFR_AF", "KG_AFR_AF", "Allele frequency in African population (1000 Genomes Phase 3)"),
				new FrequencyAnnotation("AMR_AF", "KG_AMR_AF", "Allele frequency in American population (1000 Genomes Phase 3)"),
				new FrequencyAnnotation("EAS_AF", "KG_EAS_AF", "Allele frequency in East Asian population (1000 Genomes Phase 3)"),
				new FrequencyAnnotation("EUR_AF", "KG_EUR_AF", "Allele frequency in European population (1000 Genomes Phase 3)"),
				new FrequencyAnnotation("SAS_AF", "KG_SAS_AF", "Allele frequency in South Asian population (1000 Genomes Phase 3)")
		);
	}

	@Override
	protected String getFileName(String chrom) {
		return null;
	}
}
