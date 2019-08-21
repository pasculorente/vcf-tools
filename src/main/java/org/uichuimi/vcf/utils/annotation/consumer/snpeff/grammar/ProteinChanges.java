package org.uichuimi.vcf.utils.annotation.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.annotation.consumer.snpeff.ProteinChange;

import java.util.ArrayList;
import java.util.List;

public class ProteinChanges {
	private List<ProteinChange> changes = new ArrayList<>();

	public void add(ProteinChange change) {
		this.changes.add(change);
	}

	public List<ProteinChange> getChanges() {
		return changes;
	}
}
