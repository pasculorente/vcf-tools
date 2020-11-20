package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.variant.Variant;

public class PositionFilter extends Filter {
	private final long pos;
	private final VariantFilter.Operator operator;

	public PositionFilter(long pos, VariantFilter.Operator operator) {
		this.pos = pos;
		this.operator = operator;
	}

	@Override
	boolean filter(Variant variant) {
		return operator.apply(pos, variant.getCoordinate().getPosition());
	}


	@Override
	public String toString() {
		return String.format("POSITION %s %,2d", operator.getSymbol(), pos);
	}

}
