package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.variant.Variant;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Matches a filter against all samples.
 * If mandatory, all samples must meet the condition.
 */
public class SampleFilter extends Filter {

	private final List<Integer> sampleIndices;
	private final String key;
	private final VariantFilter.Operator operator;
	private final Object value;
	private final boolean matchAll;
	private final boolean matchAllSamples;
	private final String sample;
	private final boolean mandatory;

	public SampleFilter(VcfHeader header, String sample, String key, VariantFilter.Operator operator, Object value, boolean matchAll, boolean mandatory) {
		this.key = key;
		this.operator = operator;
		this.value = value;
		this.matchAll = matchAll;
		this.matchAllSamples = !sample.equals("*");
		this.mandatory = mandatory;
		this.sampleIndices = getSampleIndices(header.getSamples(), sample);
		this.sample = sample;
	}

	private List<Integer> getSampleIndices(List<String> samples, String sample) {
		if (sample.isBlank() || sample.equals("*"))
			return IntStream.range(0, samples.size()).boxed().collect(Collectors.toList());
		else return List.of(samples.indexOf(sample));
	}

	@Override
	boolean filter(Variant variant) {
		return matchAllSamples
				? sampleIndices.stream().allMatch(index -> filter(variant, index))
				: sampleIndices.stream().anyMatch(index -> filter(variant, index));
	}

	private boolean filter(Variant variant, Integer index) {
		final Object object = variant.getSampleInfo(index).get(key);
		return filter(mandatory, matchAll, operator, object, value);
	}

	@Override
	public String toString() {
		return String.format("%s %s %s %s (%s), %s",
				sample.isBlank() ? "all" : sample.equals("*") ? "any" : sample,
				key, operator.getSymbol(), value,
				value.getClass().getSimpleName(), matchAll ? "all" : "any");
	}
}
