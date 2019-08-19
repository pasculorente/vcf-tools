package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

public class OneAlleleToken implements Token<ProteinChanges> {
	private ProteinChanges changes;

	// [VARIANT;VARIANT]
	@Override
	public boolean consume(String value) {
		if (!value.startsWith("[")) return false;
		if (!value.endsWith("]")) return false;
		final VariantToken variant1 = new VariantToken();
		if (!variant1.consume(value.substring(1))) return false;
		value = variant1.getReminder();
		if (!value.startsWith(";")) return false;
		final VariantToken variant2 = new VariantToken();
		if (!variant2.consume(value.substring(1))) return false;
		changes = new ProteinChanges();
		// TODO: 12/8/19 make variant be ProteinChange
		changes.add(variant1.getValue().getChanges().get(0));
		changes.add(variant2.getValue().getChanges().get(0));
		return true;
	}

	@Override
	public String getReminder() {
		return "";
	}

	@Override
	public ProteinChanges getValue() {
		return changes;
	}
}
