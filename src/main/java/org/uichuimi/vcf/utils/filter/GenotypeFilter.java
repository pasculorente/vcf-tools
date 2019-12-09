package org.uichuimi.vcf.utils.filter;

import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.variant.Info;
import org.uichuimi.vcf.variant.Variant;

import java.util.*;

class GenotypeFilter extends Filter {

	private static final EnumSet<VariantFilter.GT> ALL_GT = EnumSet.allOf(VariantFilter.GT.class);
	private final List<Set<VariantFilter.GT>> gts;

	GenotypeFilter(VcfHeader header, Map<String, Set<VariantFilter.GT>> gts) {
		final List<Set<VariantFilter.GT>> genotypes = new ArrayList<>();
		// Create sorted list of genotypes
		for (String sample : header.getSamples())
			genotypes.add(gts.getOrDefault(sample, Collections.emptySet()));
		this.gts = genotypes;
	}

	@Override
	boolean filter(Variant variant) {
		for (int i = 0; i < variant.getSampleInfo().size(); i++) {
			final Set<VariantFilter.GT> gts = this.gts.get(i);
			final Info info = variant.getSampleInfo().get(i);
			final boolean filter = compare(gts, info);
			if (!filter) return false;
		}
		return true;
	}

	private boolean compare(Set<VariantFilter.GT> gts, Info info) {
		if (info == null) return true;
		if (gts.isEmpty()) return true;
		if (gts.equals(ALL_GT)) return true;
		final VariantFilter.GT gt = extractGT(info);
		return gts.contains(gt);
	}

	private VariantFilter.GT extractGT(Info info) {
		final String gt = info.get("GT");
		if (gt == null || gt.contains(".")) return VariantFilter.GT.UNCALLED;
		final String[] split;
		if (gt.contains("/")) split = gt.split("/");
		else split = gt.split("\\|");
		if (split.length < 2) return VariantFilter.GT.UNCALLED;
		if (split[0].equals("0")) {
			if (split[1].equals("0")) return VariantFilter.GT.WILD;
			 else return VariantFilter.GT.HETERO;
		} else {
			if (split[1].equals(split[0])) return VariantFilter.GT.HOMO;
			else return VariantFilter.GT.HETERO;
		}
	}
}
