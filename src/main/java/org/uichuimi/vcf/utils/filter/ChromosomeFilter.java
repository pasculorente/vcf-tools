package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.variant.Variant;

public class ChromosomeFilter extends Filter {

	private final String chrom;
	private final VariantFilter.Operator operator;

	public ChromosomeFilter(String chrom, VariantFilter.Operator operator) {
		this.chrom = chrom;
		this.operator = operator;
	}

	@Override
	boolean filter(Variant variant) {
		return operator.apply(variant.getCoordinate().getChrom(), chrom);
	}

	@Override
	public String toString() {
		return String.format("CHROM %s %s", operator.getSymbol(), chrom);
	}
}
