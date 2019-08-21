package org.uichuimi.vcf.utils.annotation.consumer.snpeff.grammar;

import java.util.List;

public abstract class OptionToken<T> implements Token<T> {

	private Token<T> option;

	@Override
	public boolean consume(String value) {
		try {
			for (Class<? extends Token<T>> option : getOptions()) {
				final Token<T> token = option.newInstance();
				if (token.consume(value)) {
					this.option = token;
					return true;
				}
			}
		} catch (IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
		}
		return false;
	}

	Token<T> getOption() {
		return option;
	}

	@Override
	public String getReminder() {
		return option.getReminder();
	}

	@Override
	public T getValue() {
		return option.getValue();
	}

	abstract List<Class<? extends Token<T>>> getOptions();
}
