package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.variant.Variant;

import java.util.List;

public abstract class Filter {

	abstract boolean filter(Variant variant);

	protected boolean filter(boolean matchAll, VariantFilter.Operator operator, Object object, Object value) {
		if (object == null) return true;
		if (object instanceof List) {
			final List<?> list = (List<?>) object;
			return matchAll
					? list.stream().allMatch(obj -> obj != null && operator.apply(obj, value))
					: list.stream().anyMatch(obj -> obj != null && operator.apply(obj, value));
		}
		return operator.apply(object, value);
	}
}
