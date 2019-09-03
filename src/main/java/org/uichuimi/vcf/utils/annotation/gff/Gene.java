package org.uichuimi.vcf.utils.annotation.gff;

import java.util.ArrayList;
import java.util.List;

public class Gene extends Feature {
	private final String chromosome;
	private final String name;
	private final List<Transcript> transcripts = new ArrayList<>();

	Gene(String id, String type, String name, String biotype, String chromosome, Integer start, Integer end) {
		super(id, type, biotype, start, end);
		this.name = name;
		this.chromosome = chromosome;

	}

	public String getName() {
		return name;
	}

	public List<Transcript> getTranscripts() {
		return transcripts;
	}

	public Transcript getTranscript(long position) {
		return binarySearch(transcripts, position);
	}

	public String getChromosome() {
		return chromosome;
	}
}
