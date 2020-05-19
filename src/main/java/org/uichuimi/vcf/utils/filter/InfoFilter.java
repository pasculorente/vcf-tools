package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.variant.Variant;

public class InfoFilter extends Filter {

	private final String key;
	private final VariantFilter.Operator operator;
	private final Object value;
	private final boolean matchAll;
	private final boolean mandatory;

	public InfoFilter(String key, VariantFilter.Operator operator, Object value, boolean matchAll, boolean mandatory) {
		this.key = key;
		this.operator = operator;
		this.value = value;
		this.matchAll = matchAll;
		this.mandatory = mandatory;
	}

	@Override
	boolean filter(Variant variant) {
		final Object object = variant.getInfo(key);
		return filter(mandatory, matchAll, operator, object, value);
	}

	@Override
	public String toString() {
		return String.format("%s %s %s (%s), %s, %s", key, operator.getSymbol(), value,
				value.getClass().getSimpleName(), matchAll ? "all" : "any", (mandatory ? "" : "not ") + "mandatory");
	}
}
