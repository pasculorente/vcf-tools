package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.consumer.snpeff.Amino;

public class AminoToken implements Token<Amino> {

	private Amino value;
	private String reminder;

	@Override
	public boolean consume(String value) {
		for (Amino amino : Amino.values()) {
			if (value.startsWith(amino.getShortName())) {
				this.value = amino;
				this.reminder = value.substring(amino.getShortName().length());
				return true;
			}
		}
		// *
		if (value.startsWith(Amino.TERMINATION.getSymbol())) {
			this.value = Amino.TERMINATION;
			this.reminder = value.substring(Amino.TERMINATION.getSymbol().length());
			return true;
		}
		// =
		if (value.startsWith(Amino.IDENTICAL.getSymbol())) {
			this.value = Amino.IDENTICAL;
			this.reminder = value.substring(Amino.IDENTICAL.getSymbol().length());
			return true;
		}
		if (value.startsWith("?")) {
			this.value = Amino.UNKNOWN;
			this.reminder = value.substring("?".length());
			return true;
		}
		return false;
	}

	@Override
	public Amino getValue() {
		return value;
	}

	@Override
	public String getReminder() {
		return reminder;
	}
}
