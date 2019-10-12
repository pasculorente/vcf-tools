package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.variant.Variant;

public class InfoFilter extends Filter {

	private final String key;
	private final VariantFilter.Operator operator;
	private final Object value;
	private final boolean matchAll;

	public InfoFilter(String key, VariantFilter.Operator operator, Object value, boolean matchAll) {
		this.key = key;
		this.operator = operator;
		this.value = value;
		this.matchAll = matchAll;
	}

	@Override
	boolean filter(Variant variant) {
		final Object object = variant.getInfo(key);
		return filter(matchAll, operator, object, value);
	}

	@Override
	public String toString() {
		return String.format("%s %s %s (%s), %s", key, operator.getSymbol(), value,
				value.getClass().getSimpleName(), matchAll ? "all" : "any");
	}
}
