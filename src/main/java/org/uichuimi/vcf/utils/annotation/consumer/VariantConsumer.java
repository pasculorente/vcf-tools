package org.uichuimi.vcf.utils.annotation.consumer;

import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.utils.exception.VcfException;
import org.uichuimi.vcf.variant.Variant;

public interface VariantConsumer {

	void start(VcfHeader header) throws VcfException;

	void accept(Variant variant);

	void close();
}
