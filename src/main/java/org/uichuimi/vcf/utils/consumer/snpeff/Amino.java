package org.uichuimi.vcf.utils.consumer.snpeff;

import java.util.HashMap;
import java.util.Map;

public enum Amino {
	ALANINE("Alanine", "Ala", "A"),
	ARGININE("Arginine", "Arg", "R"),
	ASPARAGINE("Asparagine", "Asn", "N"),
	ASPARTIC("Aspartic acid", "Asp", "D"),
	ASPARTIC_OR_ASPARAGINE("Aspartic acid or Asparagine", "Asx", "B"),
	CYSTEINE("Cysteine", "Cys", "C"),
	GLUTAMIC("Glutamic acid", "Glu", "E"),
	GLUTAMIC_OR_GLUTAMINE("Glutamic acid or Glutamine", "Glx", "Z"),
	GLUTAMINE("Glutamine", "Gln", "Q"),
	GLYCINE("Glycine", "Gly", "G"),
	HISTIDINE("Histidine", "His", "H"),
	ISOLEUCINE("Isoleucine", "Ile", "I"),
	LEUCINE("Leucine", "Leu", "L"),
	LYSINE("Lysine", "Lys", "K"),
	METHIONINE("Methionine", "Met", "M"),
	PHENYLALANINE("Phenylalanine", "Phe", "F"),
	PROLINE("Proline", "Pro", "P"),
	SELENOCYSTEINE("Selenocysteine", "Sec", "U"),
	SERINE("Serine", "Ser", "S"),
	TERMINATION("Termination", "Ter", "*"),
	THREONINE("Threonine", "Thr", "T"),
	TRYPTOPHAN("Tryptophan", "Trp", "W"),
	TYROSINE("Tyrosine", "Tyr", "Y"),
	UNKNOWN("unknown or ‘other’", "Xaa", "X"),
	VALINE("Valine", "Val", "V"),
	IDENTICAL("Identical", "Ide", "=");

	private final static Map<String, Amino> SHORT_INDEX;

	static {
		SHORT_INDEX = new HashMap<>();
		for (Amino amino : values()) SHORT_INDEX.put(amino.shortName, amino);
	}

	private final String name;
	private final String shortName;
	private final String symbol;

	Amino(String name, String shortName, String symbol) {
		this.name = name;
		this.shortName = shortName;
		this.symbol = symbol;
	}

	public String getName() {
		return name;
	}

	public String getShortName() {
		return shortName;
	}

	public String getSymbol() {
		return symbol;
	}

	public static Amino fromShort(String amino) {
		return SHORT_INDEX.get(amino);
	}


}
