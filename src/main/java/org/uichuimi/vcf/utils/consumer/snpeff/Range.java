package org.uichuimi.vcf.utils.consumer.snpeff;

public class Range {

	private Amino startAmino;
	private Integer startPosition;

	private Amino endAmino;
	private Integer endPosition;

	public Amino getStartAmino() {
		return startAmino;
	}

	public void setStartAmino(Amino startAmino) {
		this.startAmino = startAmino;
	}

	public Integer getStartPosition() {
		return startPosition;
	}

	public void setStartPosition(Integer startPosition) {
		this.startPosition = startPosition;
	}

	public Amino getEndAmino() {
		return endAmino;
	}

	public void setEndAmino(Amino endAmino) {
		this.endAmino = endAmino;
	}

	public Integer getEndPosition() {
		return endPosition;
	}

	public void setEndPosition(Integer endPosition) {
		this.endPosition = endPosition;
	}

	@Override
	public String toString() {
		return startAmino.getShortName() + startPosition + (endAmino == null ? "" : "_" + endAmino.getShortName() + endPosition);
	}
}
