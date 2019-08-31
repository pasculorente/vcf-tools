package org.uichuimi.vcf.utils.annotation;

import org.junit.jupiter.api.Test;
import org.uichuimi.vcf.variant.Chromosome;

import java.io.File;
import java.util.List;

public class VariantAnnotatorTest {

	@Test
	public void debug()  {
		final File input = new File(getClass().getResource("/files/input.vcf").getFile());
		final File exac = new File(getClass().getResource("/files/ExAC.vcf").getFile());
		final File dbsnp = new File(getClass().getResource("/files/dbSNP.vcf").getFile());
		final File vep = new File(getClass().getResource("/files/vep.vcf").getFile());
		final File genes = new File(getClass().getResource("/files/Homo_sapiens.GRCh38.95.gff3.gz").getFile());
		final File gnomadGenomes = new File(getClass().getResource("/files/gnomad_genomes.vcf").getFile());
		final File gnomadExomes = new File(getClass().getResource("/files/gnomad_exomes.vcf").getFile());
		final VariantAnnotator annotator = new VariantAnnotator(List.of(input), null)
				.setNamespace(Chromosome.Namespace.UCSC)
				.setCompute(true)
				.setSnpeff(true)
				.setExac(exac)
				.setGenes(genes)
				.setGnomadExomes(gnomadExomes)
				.setGnomadGenomes(gnomadGenomes)
				.setVep(vep)
				.setDbsnp(dbsnp);
		try {
			annotator.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
