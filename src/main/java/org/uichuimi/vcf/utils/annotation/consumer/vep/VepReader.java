package org.uichuimi.vcf.utils.annotation.consumer.vep;

import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.annotation.common.FileUtils;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Reader for annotation VEP files (homo_sapiens_incl_consequences-chr%s.vcf.gz)
 */
class VepReader implements AutoCloseable {

	private final File path;
	private VariantReader reader;

	private String chromosome = null;
	private Variant current;

	private static final String FORMAT = "homo_sapiens_incl_consequences-chr%s.vcf.gz";

	/**
	 * Creates an instance of VepReader associated to path.
	 *
	 * @param path path to VEP files (homo_sapiens_incl_consequences-chr*.vcf.gz)
	 */
	public VepReader(File path) {
		this.path = path;
	}

	public Collection<Variant> getAnnotationList(Coordinate coordinate) {
		openReader(coordinate.getChrom());
		if (reader == null) return Collections.emptyList();
		if (current == null) current = reader.next();
		// vep can store 1 variant context in more than 1 line
		final List<Variant> contexts = new ArrayList<>();

		while (current != null) {
			// Compare only the position, chromosome is already the same
			final int compare = Integer.compare(current.getCoordinate().getPosition(), coordinate.getPosition());
			if (compare > 0) return contexts; // this will preserve current variant for next call
			if (compare < 0) current = reader.next();
			else { // compare == 0
				contexts.add(current);
				current = reader.next();
			}
		}
		return contexts;

	}

	private void openReader(String chrom) {
		if (chromosome != null && chromosome.equals(chrom)) return;
		try {
			close();
			final File file = new File(path, String.format(FORMAT, chrom));
			if (!file.exists()) return;
			this.reader = new VariantReader(FileUtils.getInputStream(file));
			chromosome = chrom;
			current = null;
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
