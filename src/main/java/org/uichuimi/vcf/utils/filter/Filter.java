package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.variant.Variant;

public abstract class Filter {

	abstract boolean filter(Variant variant);
}
