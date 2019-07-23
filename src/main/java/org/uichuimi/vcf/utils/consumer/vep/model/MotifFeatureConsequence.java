package org.uichuimi.vcf.utils.consumer.vep.model;

public class MotifFeatureConsequence extends Consequence {

	private String motifFeatureId;
	private String motifName;
	private Long motifPos;
	private String highInfPos;
	private Double motifScoreChange;

	public String getMotifFeatureId() {
		return motifFeatureId;
	}

	public String getMotifName() {
		return motifName;
	}

	public Long getMotifPos() {
		return motifPos;
	}

	public String getHighInfPos() {
		return highInfPos;
	}

	public Double getMotifScoreChange() {
		return motifScoreChange;
	}
}
