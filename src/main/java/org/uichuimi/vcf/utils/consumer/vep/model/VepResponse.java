package org.uichuimi.vcf.utils.consumer.vep.model;

import java.util.Collection;

public class VepResponse {

	private String input;
	private String id;
	private String mostSevereConsequence;
	private String seqRegionName;
	private String alleleString;
	private String assemblyName;

	private Long start;
	private Long end;

	private Integer strand;

	private Collection<RegulatoryFeatureConsequence> regulatoryFeatureConsequences;
	private Collection<TranscriptConsequence> transcriptConsequences;
	private Collection<ColocatedVariant> colocatedVariants;
	private Collection<IntergenicConsequence> intergenicConsequences;
	private Collection<MotifFeatureConsequence> motifFeatureConsequences;

	public Collection<TranscriptConsequence> getTranscriptConsequences() {
		return transcriptConsequences;
	}

	public String getInput() {
		return input;
	}

	public String getId() {
		return id;
	}

	public String getMostSevereConsequence() {
		return mostSevereConsequence;
	}

	public String getSeqRegionName() {
		return seqRegionName;
	}

	public String getAlleleString() {
		return alleleString;
	}

	public String getAssemblyName() {
		return assemblyName;
	}

	public Long getStart() {
		return start;
	}

	public Long getEnd() {
		return end;
	}

	public Integer getStrand() {
		return strand;
	}

	public Collection<ColocatedVariant> getColocatedVariants() {
		return colocatedVariants;
	}

	public Collection<IntergenicConsequence> getIntergenicConsequences() {
		return intergenicConsequences;
	}

	public Collection<RegulatoryFeatureConsequence> getRegulatoryFeatureConsequences() {
		return regulatoryFeatureConsequences;
	}

	public Collection<MotifFeatureConsequence> getMotifFeatureConsequences() {
		return motifFeatureConsequences;
	}
}
