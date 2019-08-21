package org.uichuimi.vcf.utils.annotation.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.annotation.consumer.snpeff.ProteinChange;

public class DeletionToken implements Token<ProteinChange> {

	private static final String DEL = "del";
	private ProteinChange value;
	private String reminder;

	@Override
	public boolean consume(String value) {
		// del
		if (value.startsWith(DEL)) {
			this.value = new ProteinChange();
			this.value.setType(ProteinChange.Type.DELETION);
			this.reminder = value.substring(DEL.length());
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
