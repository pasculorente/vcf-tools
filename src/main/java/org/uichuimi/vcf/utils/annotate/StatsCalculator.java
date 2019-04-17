package org.uichuimi.vcf.utils.annotate;

import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.utils.Genotype;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.VariantContext;

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
	public void accept(VariantContext variant, Coordinate coordinate) {
		dp(variant);
		ac(variant);
//		qd(variant);
	}

	private void dp(VariantContext variant) {
		final int dp = IntStream.range(0, header.getSamples().size())
				.mapToObj(i -> variant.getSampleInfo(i).getGlobal().getNumber("DP"))
				.filter(Objects::nonNull)
				.mapToInt(Number::intValue)
				.sum();
		variant.getInfo().getGlobal().set("DP", dp);
	}

	private void ac(VariantContext variant) {
		// Alternative allele count
		// AC, number of times an allele has been observed
		// AN, number of alleles observed, including reference
		int[] ac = new int[variant.getAlleles().size()];
		for (int s = 0; s < header.getSamples().size(); s++) {
			final String gt = variant.getSampleInfo(s).getGlobal().getString("GT");
			if (gt == null) continue;
			final Genotype genotype = Genotype.create(gt);
			ac[genotype.getA()]++;
			ac[genotype.getB()]++;
		}
		// This do take into account all alleles, including reference
		final int an = Arrays.stream(ac).sum();
		variant.getInfo().getGlobal().set("AN", an);
		// Skip the reference allele, since AC has Number=A
		for (int a = 1; a < ac.length; a++) {
			variant.getInfo().getAllele(a).set("AC", ac[a]);
			variant.getInfo().getAllele(a).set("AF", (double) ac[a] / an);
		}

	}

	private void qd(VariantContext variant) {
		final double qd = variant.getQuality() / variant.getInfo().getGlobal().getNumber("DP").intValue();
		variant.getInfo().getGlobal().set("QD", qd);
	}

	@Override
	public void close() {

	}
}
