package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.consumer.snpeff.ProteinChange;

public class InsertionToken implements Token<ProteinChange> {
	private static final String INS = "ins";
	private ProteinChange change;
	private String reminder;

	@Override
	public boolean consume(String value) {
		// ins<SEQUENCE>|ins<COPIES>
		if (!value.startsWith(INS)) return false;
		value = value.substring(INS.length());

		final SequenceToken sequence = new SequenceToken();
		if (sequence.consume(value)) {
			change = new ProteinChange();
			change.setType(ProteinChange.Type.INSERTION);
			change.setAlternative(sequence.getValue());
			reminder = sequence.getReminder();
			return true;
		}
		final CopiesToken copiesToken = new CopiesToken();
		if (copiesToken.consume(value)) {
			change = new ProteinChange();
			change.setType(ProteinChange.Type.INSERTION);
			change.setCopies(copiesToken.getValue());
			reminder = copiesToken.getReminder();
			return true;
		}
		return false;
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
