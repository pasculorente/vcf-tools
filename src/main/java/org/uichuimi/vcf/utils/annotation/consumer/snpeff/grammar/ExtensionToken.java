package org.uichuimi.vcf.utils.annotation.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.annotation.consumer.snpeff.ProteinChange;

public class ExtensionToken implements Token<ProteinChange> {
	@Override
	public boolean consume(String value) {
		return false;
	}

	@Override
	public String getReminder() {
		return null;
	}

	@Override
	public ProteinChange getValue() {
		return null;
	}
}
