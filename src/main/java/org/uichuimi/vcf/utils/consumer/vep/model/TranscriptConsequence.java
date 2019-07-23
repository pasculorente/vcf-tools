package org.uichuimi.vcf.utils.consumer.vep.model;

import java.util.Collection;

public class TranscriptConsequence extends Consequence {

	private String hgncId;
	private String geneId;
	private String geneSymbol;
	private String geneSymbolSource;

	private String aminoAcids;
	private String codons;

	private String transcriptId;
	private String biotype;

	private Collection<String> flags;

	private Long proteinStart;
	private Long proteinEnd;

	private Long cdnaStart;
	private Long cdnaEnd;

	private Long cdsStart;
	private Long cdsEnd;

	private Long distance;

	private Double siftScore;
	private String siftPrediction;

	private Double polyphenScore;
	private String polyphenPrediction;

	public String getHgncId() {
		return hgncId;
	}

	public String getGeneId() {
		return geneId;
	}

	public String getGeneSymbol() {
		return geneSymbol;
	}

	public String getGeneSymbolSource() {
		return geneSymbolSource;
	}

	public String getAminoAcids() {
		return aminoAcids;
	}

	public String getCodons() {
		return codons;
	}

	public String getTranscriptId() {
		return transcriptId;
	}

	public String getBiotype() {
		return biotype;
	}

	public Collection<String> getFlags() {
		return flags;
	}

	public Long getProteinStart() {
		return proteinStart;
	}

	public Long getProteinEnd() {
		return proteinEnd;
	}

	public Long getCdnaStart() {
		return cdnaStart;
	}

	public Long getCdnaEnd() {
		return cdnaEnd;
	}

	public Long getCdsStart() {
		return cdsStart;
	}

	public Long getCdsEnd() {
		return cdsEnd;
	}

	public Double getSiftScore() {
		return siftScore;
	}

	public String getSiftPrediction() {
		return siftPrediction;
	}

	public Double getPolyphenScore() {
		return polyphenScore;
	}

	public String getPolyphenPrediction() {
		return polyphenPrediction;
	}

	public Long getDistance() {
		return distance;
	}
}
