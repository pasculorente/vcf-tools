package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.consumer.snpeff.Amino;

import java.util.LinkedHashSet;
import java.util.Set;

public class SetToken implements Token<Set<Amino>> {

	private static final String SEPARATOR = "^";
	private Set<Amino> set;
	private String reminder;

	@Override
	public boolean consume(String value) {
		if (!value.contains(SEPARATOR)) return false;
		final String[] aminos = value.split("\\" + SEPARATOR);
		final AminoToken aminoToken = new AminoToken();
		if (!aminoToken.consume(aminos[aminos.length - 1])) return false;
		this.reminder = aminoToken.getReminder();
		aminos[aminos.length - 1] = aminoToken.getValue().getShortName();
		set = new LinkedHashSet<>();
		for (String amino : aminos) set.add(Amino.fromShort(amino));
		return true;
	}

	@Override
	public String getReminder() {
		return reminder;
	}

	@Override
	public Set<Amino> getValue() {
		return set;
	}
}
