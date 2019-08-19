package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.consumer.snpeff.Amino;

public class AminoPosToken implements Token<Amino> {

	private String reminder;
	private Amino amino;
	private Integer position;

	@Override
	public boolean consume(String value) {
		final AminoToken aminoToken = new AminoToken();
		if (!aminoToken.consume(value)) return false;
		final PositionToken positionToken = new PositionToken();
		if (!positionToken.consume(aminoToken.getReminder())) return false;
		this.amino = aminoToken.getValue();
		this.position = positionToken.getValue();
		this.reminder = positionToken.getReminder();
		return true;
	}

	@Override
	public Amino getValue() {
		return amino;
	}

	public Integer getPosition() {
		return position;
	}

	public String getReminder() {
		return reminder;
	}
}
