package org.uichuimi.vcf.utils.commands;

import org.uichuimi.vcf.header.InfoHeaderLine;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.utils.annotation.BaseCommand;
import org.uichuimi.vcf.variant.Variant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.function.BiConsumer;

@Command(name = "set-line-number", description = "Creates a tag or sets the ID col to the line number")
public class SetLineNumber extends BaseCommand {

	@Option(names = {"--tag", "-t"},
			description = "Name of the INFO/tag where to write the number. If not set, writes to ID column")
	private String tag;

	private BiConsumer<Variant, Integer> consumer;

	@Override
	protected void setUp(VcfHeader header) {
		if (tag != null) {
			header.addHeaderLine(new InfoHeaderLine(tag, "1", "Integer", "Line number"));
			consumer = ((variant, line) -> variant.setInfo(tag, line));
		} else {
			consumer = (variant, line) -> {
				variant.setIdentifiers(List.of(String.valueOf(line)));
			};
		}
	}

	@Override
	protected void consume(Variant variant, long line) {
		consumer.accept(variant, (int) line);
	}

	@Override
	protected void tearDown() {

	}
}
