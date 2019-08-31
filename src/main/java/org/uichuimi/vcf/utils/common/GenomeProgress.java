package org.uichuimi.vcf.utils.common;

import org.uichuimi.vcf.variant.Chromosome;
import org.uichuimi.vcf.variant.ChromosomeFactory;
import org.uichuimi.vcf.variant.Coordinate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple utility to know the approximate progress in the genome based on a coordinate.
 */
public class GenomeProgress {

	private final static List<Chromosome> CHROMOSOMES = ChromosomeFactory.getChromosomeList();
	private final static Map<Chromosome, Long> abs = new HashMap<>();
	private final static Long total;

	static {
		long t = 0L;
		for (Chromosome chromosome : CHROMOSOMES) {
			abs.put(chromosome, t);
			t += chromosome.getLength();
		}
		total = t;
	}
	
	public static double getProgress(Coordinate coordinate) {
		final Long base = abs.get(coordinate.getChromosome());
		if (base == null) return 0.99;
		return (base + coordinate.getPosition()) / ( double) total;
	}

}
