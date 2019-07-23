package org.uichuimi.vcf.utils.consumer.vep.model;

import java.util.Collection;

public class Consequence {

	private Collection<String> consequenceTerms;
	private String impact;
	private String variantAllele;

	private Integer strand;


	public String getImpact() {
		return impact;
	}

	public Collection<String> getConsequenceTerms() {
		return consequenceTerms;
	}

	public String getVariantAllele() {
		return variantAllele;
	}

	public Integer getStrand() {
		return strand;
	}
}
