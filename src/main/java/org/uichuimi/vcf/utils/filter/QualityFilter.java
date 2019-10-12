package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.variant.Variant;

public class QualityFilter extends Filter {
	private final double qual;
	private final VariantFilter.Operator operator;

	public QualityFilter(double qual, VariantFilter.Operator operator) {
		this.qual = qual;
		this.operator = operator;
	}

	@Override
	boolean filter(Variant variant) {
		return operator.apply(variant.getQuality(), qual);
	}
}
