package org.uichuimi.vcf.utils.annotation.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.annotation.consumer.snpeff.ProteinChange;

public class RepeatedToken implements Token<ProteinChange> {
	private static final String CLOSURE = "]";
	private static final String OPENING = "[";
	private ProteinChange change;
	private String reminder;

	@Override
	public boolean consume(String value) {
		// [COPIES]
		if (!value.startsWith(OPENING)) return false;
		final CopiesToken copiesToken = new CopiesToken();
		if (!copiesToken.consume(value.substring(OPENING.length()))) return false;
		value = copiesToken.getReminder();
		if (!value.startsWith(CLOSURE)) return false;
		change = new ProteinChange();
		change.setType(ProteinChange.Type.REPEATED);
		change.setCopies(copiesToken.getValue());
		reminder = value.substring(CLOSURE.length());
		return true;
	}

	@Override
	public String getReminder() {
		return reminder;
	}

	@Override
	public ProteinChange getValue() {
		return change;
	}
}
