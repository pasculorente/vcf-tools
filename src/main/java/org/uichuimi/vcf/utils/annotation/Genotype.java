package org.uichuimi.vcf.utils.annotation;

public class Genotype {

	private final static String PHASED = "|";
	private final static String UNPHASED = "/";
	private final int a;
	private final int b;
	private final boolean phased;
	private final String separator;
	private final Type type;

	private Genotype(int a, int b, boolean phased) {
		this.a = a;
		this.b = b;
		this.phased = phased;
		separator = phased ? PHASED : UNPHASED;
		type = a != b ? Type.HETEROZYGOUS : a == 0 ? Type.WILDTYPE : Type.HOMOZYGOUS;
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
		return new Genotype(Integer.parseInt(values[0]), Integer.parseInt(values[1]), phased);
	}

	public int getA() {
		return a;
	}

	public int getB() {
		return b;
	}

	public boolean isPhased() {
		return phased;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return a + separator + b;
	}

	public enum  Type {
		WILDTYPE, HETEROZYGOUS, HOMOZYGOUS
	}
}
