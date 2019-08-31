package org.uichuimi.vcf.utils.annotation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uichuimi.vcf.variant.Chromosome;

import java.io.*;
import java.util.List;

public class VariantAnnotatorTest {

	@Test
	public void debug() {
		final File input = new File(getClass().getResource("/files/input.vcf").getFile());
		final File exac = new File(getClass().getResource("/files/ExAC.vcf").getFile());
		final File dbsnp = new File(getClass().getResource("/files/dbSNP.vcf").getFile());
		final File vep = new File(getClass().getResource("/files/vep.vcf").getFile());
		final File genes = new File(getClass().getResource("/files/Homo_sapiens.GRCh38.95.gff3.gz").getFile());
		final File gnomadGenomes = new File(getClass().getResource("/files/gnomad_genomes.vcf").getFile());
		final File gnomadExomes = new File(getClass().getResource("/files/gnomad_exomes.vcf").getFile());
		final byte[] data = generateOutputData(input, exac, dbsnp, vep, genes, gnomadGenomes, gnomadExomes);
		final InputStream expected = getClass().getResourceAsStream("/files/expected_output.vcf");
		try (BufferedReader dataReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
		     BufferedReader expectedReader = new BufferedReader(new InputStreamReader(expected))) {
			String dataLine;
			String expectedLine;
			do {
				dataLine = dataReader.readLine();
				expectedLine = expectedReader.readLine();
				Assertions.assertEquals(expectedLine, dataLine);
			} while (dataLine != null && expectedLine != null);
			if (dataLine != null)
				Assertions.fail("Output contains more lines than expected: " + dataLine);
			if (expectedLine != null) Assertions.fail("Missing output lines: " + expectedLine);
		} catch (IOException e) {
			Assertions.fail(e);
		}
	}

	private byte[] generateOutputData(File input, File exac, File dbsnp, File vep, File genes, File gnomadGenomes, File gnomadExomes) {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			final VariantAnnotator annotator = new VariantAnnotator(List.of(input), null)
					.setNamespace(Chromosome.Namespace.UCSC)
					.setCompute(true)
					.setSnpeff(true)
					.setExac(exac)
					.setGenes(genes)
					.setGnomadExomes(gnomadExomes)
					.setGnomadGenomes(gnomadGenomes)
					.setVep(vep)
					.setDbsnp(dbsnp)
					.setOutputStream(os);
			annotator.call();
			return os.toByteArray();
		} catch (Exception e) {
			Assertions.fail(e);
		}
		return new byte[0];
	}
}
