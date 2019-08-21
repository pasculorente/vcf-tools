package org.uichuimi.vcf.utils.annotation.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.annotation.consumer.snpeff.ProteinChange;

public class PredictionToken implements Token<ProteinChange> {

	public static final String OPENING = "(";
	public static final String CLOSING = ")";
	private ProteinChange value = null;
	private String reminder;

	@Override
	public boolean consume(String value) {
		// (CHANGE)
		if (!value.startsWith(OPENING)) return false;
		final ChangeToken changeToken = new ChangeToken();
		if (!changeToken.consume(value.substring(OPENING.length()))) return false;
		value = changeToken.getReminder();
		if (!value.startsWith(CLOSING)) return false;
		this.value = changeToken.getValue();
		this.value.setPredicted(true);
		reminder = value.substring(CLOSING.length());
		return true;
	}

	@Override
	public ProteinChange getValue() {
		return value;
	}

	@Override
	public String getReminder() {
		return reminder;
	}
}
