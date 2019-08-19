package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.consumer.snpeff.ProteinChange;

public class ChangeToken implements Token<ProteinChange> {
	private ProteinChange change;
	private String reminder;

	@Override
	public boolean consume(String value) {
		// 0|?|<RANGE><ALTERNATIVE>
		if (value.startsWith("0")) {
			change = new ProteinChange();
			change.setType(ProteinChange.Type.NO_PROTEIN);
			reminder = value.substring(1);
			return true;
		} else if (value.startsWith("?")) {
			change = new ProteinChange();
			change.setType(ProteinChange.Type.UNKNOWN);
			reminder = value.substring(1);
			return true;
		}
		final RangeToken rangeToken = new RangeToken();
		if (!rangeToken.consume(value)) return false;
		final AlternativeToken alternativeToken = new AlternativeToken();
		if (!alternativeToken.consume(rangeToken.getReminder())) return false;
		change = alternativeToken.getValue();
		change.setReference(rangeToken.getValue());
		reminder = alternativeToken.getReminder();
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
