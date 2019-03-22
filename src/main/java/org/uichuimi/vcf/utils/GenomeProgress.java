package org.uichuimi.vcf.utils;

import org.uichuimi.variant.io.vcf.Coordinate;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple utility to know the approximate progress in the genome based on a coordinate.
 */
public class GenomeProgress {

	private final static Map<String, Long> map = new HashMap<>();
	private final static Map<String, Long> abs = new HashMap<>();
	private final static Long total;
	static {
		map.put("1", 248_956_422L);
		map.put("2", 242_193_529L);
		map.put("3", 198_295_559L);
		map.put("4", 190_214_555L);
		map.put("5", 181_538_259L);
		map.put("6", 170_805_979L);
		map.put("7", 159_345_973L);
		map.put("8", 145_138_636L);
		map.put("9", 138_394_717L);
		map.put("10", 133_797_422L);
		map.put("11", 135_086_622L);
		map.put("12", 133_275_309L);
		map.put("13", 114_364_328L);
		map.put("14", 107_043_718L);
		map.put("15", 101_991_189L);
		map.put("16", 90_338_345L);
		map.put("17", 83_257_441L);
		map.put("18", 80_373_285L);
		map.put("19", 58_617_616L);
		map.put("20", 64_444_167L);
		map.put("21", 46_709_983L);
		map.put("22", 50_818_468L);
		map.put("X", 156_040_895L);
		map.put("Y", 57_227_415L);
		map.put("MT", 16_569L);
		long t = 0L;
		for (String chromosome : Constants.STANDARD_CHROMOSOMES) {
			abs.put(chromosome, t);
			t += map.get(chromosome);
		}
		total = t;
	}
	
	public static double getProgress(Coordinate coordinate) {
		final Long base = abs.get(coordinate.getChrom());
		if (base == null) return 0.99;
		return (base + coordinate.getPosition()) / ( double) total;
	}

}
