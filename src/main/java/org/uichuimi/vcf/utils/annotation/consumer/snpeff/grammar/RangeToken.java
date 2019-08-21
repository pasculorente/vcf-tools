package org.uichuimi.vcf.utils.annotation.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.annotation.consumer.snpeff.Range;

public class RangeToken implements Token<Range> {

	private static final String SEPARATOR = "_";
	private Range range;
	private String reminder;

	@Override
	public boolean consume(String value) {
		// AMINO_POS|<AMINO_POS>_<AMINO_POS>
		final AminoPosToken start = new AminoPosToken();
		if (!start.consume(value)) return false;
		value = start.getReminder();
		if (!value.startsWith(SEPARATOR)) {
			reminder = value;
			range = new Range();
			range.setStartPosition(start.getPosition());
			range.setStartAmino(start.getValue());
			return true;
		};
		value = value.substring(SEPARATOR.length());
		final AminoPosToken end = new AminoPosToken();
		if (!end.consume(value)) return false;
		reminder = end.getReminder();
		range = new Range();
		range.setStartAmino(start.getValue());
		range.setStartPosition(start.getPosition());
		range.setEndAmino(end.getValue());
		range.setEndPosition(end.getPosition());
		return true;
	}

	@Override
	public String getReminder() {
		return reminder;
	}

	@Override
	public Range getValue() {
		return range;
	}
}
