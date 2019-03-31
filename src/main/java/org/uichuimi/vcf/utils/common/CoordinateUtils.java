package org.uichuimi.vcf.utils.common;

import org.uichuimi.vcf.variant.Coordinate;

import java.util.HashMap;
import java.util.Map;

public class CoordinateUtils {

	private static final Map<String, String> UCSC_TO_GRCH38 = new HashMap<>();

	static {
		for (int i = 0; i <= 22; i++) UCSC_TO_GRCH38.put("chr" + i, String.valueOf(i));
		UCSC_TO_GRCH38.put("chrX", "X");
		UCSC_TO_GRCH38.put("chrY", "Y");
		UCSC_TO_GRCH38.put("chrM", "MT");
	}


	private CoordinateUtils() {
	}

	public static Coordinate toGrch38(Coordinate coordinate) {
		final String chr = UCSC_TO_GRCH38.getOrDefault(coordinate.getChrom(), coordinate.getChrom());
		return new Coordinate(chr, coordinate.getPosition());

	}
}
