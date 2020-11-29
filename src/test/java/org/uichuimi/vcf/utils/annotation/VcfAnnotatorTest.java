package org.uichuimi.vcf.utils.annotation;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.annotation.consumer.VcfWriter;
import org.uichuimi.vcf.variant.Chromosome;
import org.uichuimi.vcf.variant.Variant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

public class VcfAnnotatorTest {

	@Test
	public void annotate() throws IOException {
		final InputStream resource = getClass().getResourceAsStream("/input/vcf_annotator_ann.vcf");
		final List<ColumnSpec> columnSpecs = List.of(new ColumnSpec("CONS", "CONS"), new ColumnSpec("AN", "AN"));
		final VcfAnnotator annotator = new VcfAnnotator(resource, columnSpecs);
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (VariantReader reader = new VariantReader(getClass().getResourceAsStream("/input/vcf_annotator_variants.vcf"))) {
			final VcfWriter writer = new VcfWriter(outputStream, Chromosome.Namespace.GRCH);
			annotator.start(reader.getHeader());
			writer.start(reader.getHeader());
			for (Variant variant : reader) {
				annotator.accept(variant);
				writer.accept(variant);
			}
			writer.close();
		}
		final InputStream outputResource = getClass().getResourceAsStream("/output/vcf_annotator_output.vcf");
		final String expected = IOUtils.toString(outputResource, Charset.defaultCharset());
		Assertions.assertEquals(expected, outputStream.toString());
	}
}
