package org.uichuimi.vcf.utils;

import org.uichuimi.variant.io.vcf.Variant;
import org.uichuimi.variant.io.vcf.io.CustomVariantSetReader;
import org.uichuimi.variant.io.vcf.io.VariantSetReader;

import java.io.File;
import java.io.IOException;

public class VepReader {

	private File path;
	private VariantSetReader reader;

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

	public Variant getAnnotations(Variant variant) {
		openReader(variant.getChrom());
		if (reader == null) return null;
		if (current == null) current = reader.next();
		while (current != null) {
			final int compare = Integer.compare(current.getPosition(), variant.getPosition());
			if (compare > 0) return null; // this will preserve current variant for next call
			if (compare < 0) {
				current = reader.next();
				continue;
			}
			// compare == 0
			// We check if ref is the same
			if (!variant.getRef().equals(current.getRef())) {
				current = reader.next();
				continue;
			}
			// And at least one of the alts is shared
			final String[] cArray = current.getAltArray();
			final String[] vArray = variant.getAltArray();
			for (String v : vArray)
				for (String c : cArray)
					if (v.equals(c)) return current;
			current = reader.next();
		}
		return null;
	}

	private void openReader(String chrom) {
		if (chromosome != null && chromosome.equals(chrom)) return;
		try {
			if (reader != null) {
				reader.close();
				reader  = null;
			}
			final File file = new File(path, String.format(FORMAT, chrom));
			if (!file.exists()) return;
			final CustomVariantSetReader reader = new CustomVariantSetReader(FileUtils.getInputStream(file));
			reader.setloadId(true);
			reader.addInfo("CSQ");
			reader.addInfo("Sift");
			reader.addInfo("Polyphen");
			reader.addInfo("VarPep");
			reader.addInfo("RefPep");
			reader.addInfo("EAS_AF");
			reader.addInfo("SAS_AF");
			reader.addInfo("EUR_AF");
			reader.addInfo("AFR_AF");
			reader.addInfo("AMR_AF");
			this.reader = reader;
			chromosome = chrom;
			current = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
