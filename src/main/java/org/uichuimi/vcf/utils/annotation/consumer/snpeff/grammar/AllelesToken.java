package org.uichuimi.vcf.utils.annotation.consumer.snpeff.grammar;

import java.util.List;

public class AllelesToken extends OptionToken<ProteinChanges> {
	private static final List<Class<? extends Token<ProteinChanges>>> OPTIONS = List.of(
			VariantToken.class, OneAlleleToken.class, TwoAlleles.class, TwoChanges.class
	);

	@Override
	List<Class<? extends Token<ProteinChanges>>> getOptions() {
		return OPTIONS;
	}
}
