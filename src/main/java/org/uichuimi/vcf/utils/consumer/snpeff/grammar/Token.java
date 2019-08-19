package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

/**
 * A token is a class which reads a string and returns a value. If the format of the string is not
 * valid or the string is not consumed till the end, returns null.
 */
public interface Token<T> {

	boolean consume(String value);

	String getReminder();

	T getValue();
}
