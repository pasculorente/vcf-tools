package org.uichuimi.vcf.utils.exception;

public class VcfException extends Throwable {

	public VcfException(String msg) {
		super(msg);
	}

	public VcfException(String msg, Throwable throwable) {
		super(msg, throwable);
	}
}
