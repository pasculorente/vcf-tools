package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

public class VariantToken implements Token<ProteinChanges> {

	private ProteinChanges changes;
	private String reminder;

	@Override
	public boolean consume(String value) {
		// PREDICTION|CHANGE
		final PredictionToken prediction = new PredictionToken();
		if (prediction.consume(value)) {
			changes = new ProteinChanges();
			changes.add(prediction.getValue());
			reminder = prediction.getReminder();
			return true;
		}
		final ChangeToken changeToken = new ChangeToken();
		if (changeToken.consume(value)) {
			changes = new ProteinChanges();
			changes.add(changeToken.getValue());
			reminder = changeToken.getReminder();
			return true;
		}
		return false;
	}

	@Override
	public String getReminder() {
		return reminder;
	}

	@Override
	public ProteinChanges getValue() {
		return changes;
	}
}
