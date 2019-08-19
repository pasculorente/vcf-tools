package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.consumer.snpeff.Amino;

import java.util.ArrayList;

public class SequenceToken implements Token<ArrayList<Amino>> {

	private String reminder;
	private ArrayList<Amino> sequence;

	@Override
	public boolean consume(String value) {
		final ArrayList<Amino> aminoAcids = new ArrayList<>();
		while (true) {
			final AminoToken aminoToken = new AminoToken();
			if (!aminoToken.consume(value)) break;
			aminoAcids.add(aminoToken.getValue());
			value = aminoToken.getReminder();
		}
		if (aminoAcids.isEmpty()) return false;
		sequence = aminoAcids;
		reminder = value;
		return true;
	}

	@Override
	public String getReminder() {
		return reminder;
	}

	@Override
	public ArrayList<Amino> getValue() {
		return sequence;
	}
}
