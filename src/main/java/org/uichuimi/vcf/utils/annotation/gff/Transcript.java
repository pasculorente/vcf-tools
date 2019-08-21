package org.uichuimi.vcf.utils.annotation.gff;

public class Transcript extends Feature {

	private final Gene gene;

	Transcript(String id, String type, Gene gene, String biotype, Integer start, Integer end) {
		super(id, type, biotype, start, end);
		this.gene = gene;
		this.gene.getTranscripts().add(this);
	}

	public Gene getGene() {
		return gene;
	}
}
