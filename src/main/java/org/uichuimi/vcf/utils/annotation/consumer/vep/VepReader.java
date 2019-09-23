package org.uichuimi.vcf.utils.annotation.consumer.vep;

import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.variant.Chromosome;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * Reader for annotation VEP files (homo_sapiens_incl_consequences-chr%s.vcf.gz)
 */
class VepReader implements AutoCloseable {

	private final File path;
	private VariantReader reader;

	private Chromosome chromosome = null;

	private static final String FORMAT = "homo_sapiens_incl_consequences-chr%s.vcf.gz";

	/**
	 * Creates an instance of VepReader associated to path.
	 *
	 * @param path path to VEP files (homo_sapiens_incl_consequences-chr*.vcf.gz)
	 */
	VepReader(File path) {
		this.path = path;
	}

	Collection<Variant> getAnnotationList(Coordinate coordinate) {
		openReader(coordinate.getChromosome());
		if (reader == null) return Collections.emptyList();
		if (reader.hasNext()) return reader.nextCollected(coordinate);
		return Collections.emptyList();
	}

	private void openReader(Chromosome chrom) {
		if (chromosome != null && chromosome.equals(chrom)) return;
		try {
			close();
			final File file = new File(path, String.format(FORMAT, chrom.getName()));
			if (!file.exists()) return;
			this.reader = new VariantReader(FileUtils.getInputStream(file));
			chromosome = chrom;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws Exception {
		if (reader != null) {
			reader.close();
			reader = null;
		}
	}
}
