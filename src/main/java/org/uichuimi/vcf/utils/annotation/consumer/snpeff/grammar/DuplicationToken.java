package org.uichuimi.vcf.utils.annotation.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.annotation.consumer.snpeff.ProteinChange;

public class DuplicationToken implements Token<ProteinChange> {
	private static final String DUP = "dup";
	private ProteinChange value;
	private String reminder;

	@Override
	public boolean consume(String value) {
		// dup
		if (value.startsWith(DUP)) {
			this.value = new ProteinChange();
			this.value.setType(ProteinChange.Type.DUPLICATION);
			reminder = value.substring(DUP.length());
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
		return value;
	}
}
