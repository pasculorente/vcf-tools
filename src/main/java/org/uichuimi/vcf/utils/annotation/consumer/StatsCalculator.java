package org.uichuimi.vcf.utils.annotation.consumer;

import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.utils.annotation.Genotype;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

public class StatsCalculator implements VariantConsumer {

	private VcfHeader header;

	@Override
	public void start(VcfHeader header) {
		this.header = header;
	}

	@Override
	public void accept(Variant variant, Coordinate grch38) {
		dp(variant);
		ac(variant);
//		qd(variant);
	}

	private void dp(Variant variant) {
		final long dp = IntStream.range(0, header.getSamples().size())
				.mapToLong(i -> variant.getSampleInfo(i).get("DP"))
				.filter(Objects::nonNull)
				.sum();
		variant.setInfo("DP", dp);
	}

	private void ac(Variant variant) {
		// Alternative allele count
		// AC, number of times an allele has been observed
		// AN, number of alleles observed, including reference
		int[] ac = new int[variant.getAlleles().size()];
		for (int s = 0; s < header.getSamples().size(); s++) {
			final String gt = variant.getSampleInfo(s).get("GT");
			if (gt == null) continue;
			final Genotype genotype = Genotype.create(gt);
			ac[genotype.getA()]++;
			ac[genotype.getB()]++;
		}
		// This do take into account all alleles, including reference
		final int an = Arrays.stream(ac).sum();
		variant.setInfo("AN", an);
		// Skip the reference allele, since AC has Number=A
		final Integer[] acArray = new Integer[variant.getAlternatives().size()];
		final Float[] afArray = new Float[variant.getAlternatives().size()];
		for (int a = 1; a < ac.length; a++) {
			acArray[a] = ac[a];
			afArray[a] = (float) ac[a] / an;
		}
		variant.setInfo("AC", Arrays.asList(acArray));
		variant.setInfo("AF", Arrays.asList(afArray));

	}

	@Override
	public void close() {

	}
}
