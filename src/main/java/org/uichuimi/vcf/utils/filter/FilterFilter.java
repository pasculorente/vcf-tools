package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.variant.Variant;

public class FilterFilter extends Filter {
	private final String value;
	private final VariantFilter.Operator op;
	private final boolean matchAll;

	public FilterFilter(String value, VariantFilter.Operator op, boolean matchAll) {
		this.value = value;
		this.op = op;
		this.matchAll = matchAll;
	}

	@Override
	boolean filter(Variant variant) {
		return matchAll
				? variant.getFilters().stream().allMatch(filter -> op.apply(filter, value))
				: variant.getFilters().stream().anyMatch(filter -> op.apply(filter, value));
	}
}
