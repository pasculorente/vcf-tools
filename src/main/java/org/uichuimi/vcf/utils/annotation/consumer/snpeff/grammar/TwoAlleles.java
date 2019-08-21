package org.uichuimi.vcf.utils.annotation.consumer.snpeff.grammar;

public class TwoAlleles implements Token<ProteinChanges> {

	@Override
	public boolean consume(String value) {
		return false;
	}

	@Override
	public String getReminder() {
		return null;
	}

	@Override
	public ProteinChanges getValue() {
		return null;
	}
}
