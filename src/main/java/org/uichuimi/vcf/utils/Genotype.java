package org.uichuimi.vcf.utils;

public class Genotype {

	private final static String PHASED = "|";
	private final static String UNPHASED = "/";
	private final Integer a;
	private final Integer b;
	private final boolean phased;
	private final String separator;

	private Genotype(Integer a, Integer b, boolean phased) {
		this.a = a;
		this.b = b;
		this.phased = phased;
		separator = phased ? PHASED : UNPHASED;
	}

	public static Genotype create(String value) {
		boolean phased;
		String[] values;
		if (value.contains(PHASED)) {
			values = value.split("\\|");
			phased = true;
		} else if (value.contains(UNPHASED)) {
			values = value.split("/");
			phased = false;
		} else throw new IllegalArgumentException("Genotype not recognized " + value);
		return new Genotype(Integer.valueOf(values[0]), Integer.valueOf(values[1]), phased);
	}

	public Integer getA() {
		return a;
	}

	public Integer getB() {
		return b;
	}

	public boolean isPhased() {
		return phased;
	}

	@Override
	public String toString() {
		return a + separator + b;
	}
}
