package org.uichuimi.vcf.utils.annotation.consumer;

import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.io.VariantWriter;
import org.uichuimi.vcf.utils.common.FileUtils;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class VcfWriter implements VariantConsumer {

	private final VariantWriter writer;

	/**
	 * @param file output file
	 */
	public VcfWriter(final File file) throws IOException {
		writer = new VariantWriter(FileUtils.getOutputStream(file));
	}

	public VcfWriter(OutputStream out) {
		writer = new VariantWriter(out);
	}

	@Override
	public void start(VcfHeader header) {
		writer.setHeader(header);
	}

	@Override
	public void accept(Variant variant) {
		writer.write(variant);
	}

	@Override
	public void close() {
		try {
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
