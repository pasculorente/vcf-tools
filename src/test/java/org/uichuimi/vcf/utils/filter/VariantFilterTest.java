package org.uichuimi.vcf.utils.filter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.Main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class VariantFilterTest {

	@Test
	public void symbol() throws IOException {
		final InputStream input = getClass().getResourceAsStream("/filter/filter_input.vcf");
		final Path output = Files.createTempFile("filter_output_", ".vcf");
		System.setIn(input);
		Main.main(new String[]{"filter", "-f", "SYMBOL=A",  "-o", output.toAbsolutePath().toString()});
		try (VariantReader reader = new VariantReader(output.toFile())) {
			Assertions.assertEquals(1, reader.variants().count());
		}
	}

	@Test
	public void nullsAllowed() throws IOException {
		final InputStream input = getClass().getResourceAsStream("/filter/filter_input.vcf");
		final Path output = Files.createTempFile("filter_output_", ".vcf");
		System.setIn(input);
		Main.main(new String[]{"filter", "-f", "SYMBOL?=A",  "-o", output.toAbsolutePath().toString()});
		try (VariantReader reader = new VariantReader(output.toFile())) {
			Assertions.assertEquals(2, reader.variants().count());
		}
	}

}