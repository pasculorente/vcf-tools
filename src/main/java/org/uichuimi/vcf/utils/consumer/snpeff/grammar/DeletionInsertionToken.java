package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.consumer.snpeff.ProteinChange;

public class DeletionInsertionToken implements Token<ProteinChange> {

	private static final String DELINS = "delins";
	private ProteinChange change;
	private String reminder;

	@Override
	public boolean consume(String value) {
		// delins<SEQUENCE>
		if (!value.startsWith(DELINS)) return false;
		value = value.substring(DELINS.length());
		final SequenceToken sequence = new SequenceToken();
		if (!sequence.consume(value)) return false;
		change = new ProteinChange();
		change.setAlternative(sequence.getValue());
		change.setType(ProteinChange.Type.DELINS);
		reminder = sequence.getReminder();
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
