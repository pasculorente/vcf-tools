package org.uichuimi.vcf.utils;

import picocli.CommandLine;

import java.util.concurrent.Callable;

import static picocli.CommandLine.*;

@Command(name = "vcf-utils",
		description = "umpteenth package with tools to work with vcf files",
		subcommands = VariantContextAnnotator.class)
public class Main implements Callable<Void> {

	public static void main(String[] args) {
		Main cli = new Main();
		final CommandLine cmd = new CommandLine(cli);
		cmd.parseWithHandler(new RunLast(), args);
	}

	@Override
	public Void call() {
		usage(new Main(), System.out);
		return null;
	}
}
