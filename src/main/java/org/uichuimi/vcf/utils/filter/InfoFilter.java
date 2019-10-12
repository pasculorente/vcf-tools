package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.variant.Variant;

import java.util.List;

public class InfoFilter extends Filter {

	private final String key;
	private final VariantFilter.Operator operator;
	private final Object value;
	private final boolean matchAll;

	public InfoFilter(String key, VariantFilter.Operator operator, Object value, boolean matchAll) {
		super();
		this.key = key;
		this.operator = operator;
		this.value = value;
		this.matchAll = matchAll;
		System.out.printf("%s %s %s (%s), %s%n", key, operator.getSymbol(), value, value.getClass().getName(), matchAll);
	}

	@Override
	boolean filter(Variant variant) {
		final Object object = variant.getInfo(key);
		if (object == null) return true;
		if (object instanceof List) {
			if (matchAll) {
				for (Object element : (List) object) {
					if (element == null || !operator.apply(element, value))
						return false;
				}
				return true;
			} else {
				for (Object element : (List) object) {
					if (element == null) continue;
					if (operator.apply(element, value)) return true;
				}
				return false;
			}
		}
		return operator.apply(object, value);
	}

}
