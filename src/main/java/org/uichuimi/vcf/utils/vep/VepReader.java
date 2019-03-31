package org.uichuimi.vcf.utils.vep;

import org.uichuimi.vcf.input.VariantContextReader;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.VariantContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Reader for annotation VEP files (homo_sapiens_incl_consequences-chr%s.vcf.gz)
 */
public class VepReader implements AutoCloseable {

	private File path;
	private VariantContextReader reader;

	private String chromosome = null;
	private VariantContext current;

	private static final String FORMAT = "homo_sapiens_incl_consequences-chr%s.vcf.gz";

	/**
	 * Creates an instance of VepReader associated to path.
	 *
	 * @param path path to VEP files (homo_sapiens_incl_consequences-chr*.vcf.gz)
	 */
	public VepReader(File path) {
		this.path = path;
	}

	public Collection<VariantContext> getAnnotationList(Coordinate coordinate) {
		openReader(coordinate.getChrom());
		if (reader == null) return Collections.emptyList();
		if (current == null) current = reader.next();
		// vep can store 1 variant context in more than 1 line
		final List<VariantContext> contexts = new ArrayList<>();

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

	public VariantContext getAnnotations(Coordinate coordinate) {
		return join(getAnnotationList(coordinate));
	}

	private VariantContext join(Collection<VariantContext> contexts) {
		return null;
	}

	private void openReader(String chrom) {
		if (chromosome != null && chromosome.equals(chrom)) return;
		try {
			close();
			final File file = new File(path, String.format(FORMAT, chrom));
			if (!file.exists()) return;
			this.reader = new VariantContextReader(FileUtils.getInputStream(file));
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
