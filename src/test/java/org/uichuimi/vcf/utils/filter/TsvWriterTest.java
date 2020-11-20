package org.uichuimi.vcf.utils.filter;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.variant.Variant;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

class TsvWriterTest {

	@Test
	public void test() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final InputStream resource = getClass().getResourceAsStream("/input/tsv_writer_input.vcf");
		try (VariantReader reader = new VariantReader(resource);
		     TsvWriter writer = new TsvWriter(outputStream)) {
			writer.setHeader(reader.getHeader());
			for (Variant variant : reader) writer.write(variant);
		}
		final String actual = outputStream.toString();
		final String expected = IOUtils.toString(getClass().getResourceAsStream("/output/tsv_writer_output.tsv"), Charset.defaultCharset());
		Assertions.assertEquals(expected, actual);
	}

	@Test
	public void large() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final InputStream resource = getClass().getResourceAsStream("/input/tsv_writer_large.vcf");
		try (VariantReader reader = new VariantReader(resource);
		     TsvWriter writer = new TsvWriter(outputStream)) {
			writer.setHeader(reader.getHeader());
			for (Variant variant : reader) writer.write(variant);
		}
		final String actual = outputStream.toString();
		final String expected = IOUtils.toString(getClass().getResourceAsStream("/output/tsv_writer_large.tsv"), Charset.defaultCharset());
		Assertions.assertEquals(expected, actual);
	}

}