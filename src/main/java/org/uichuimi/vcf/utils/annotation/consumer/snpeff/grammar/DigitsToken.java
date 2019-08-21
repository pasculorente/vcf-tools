package org.uichuimi.vcf.utils.annotation.consumer.snpeff.grammar;

public class DigitsToken implements Token<Integer> {

	private String reminder;
	private Integer value = null;

	@Override
	public boolean consume(String value) {
		int i = 0;
		while (i < value.length() && Character.isDigit(value.charAt(i))) i += 1;
		if (i == 0) return false;
		this.value = Integer.valueOf(value.substring(0, i));
		this.reminder = value.substring(i);
		return true;
	}

	@Override
	public String getReminder() {
		return reminder;
	}

	@Override
	public Integer getValue() {
		return value;
	}
}
