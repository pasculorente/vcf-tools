package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.consumer.snpeff.ProteinChange;

import java.util.List;

class AlternativeToken extends OptionToken<ProteinChange> {

	private static final List<Class<? extends Token<ProteinChange>>> OPTIONS = List.of(
			DeletionInsertionToken.class, DeletionToken.class, FrameShiftToken.class,
			SubstitutionToken.class, InsertionToken.class, DuplicationToken.class,
			RepeatedToken.class, ExtensionToken.class);

	@Override
	List<Class<? extends Token<ProteinChange>>> getOptions() {
		return OPTIONS;
	}
}
