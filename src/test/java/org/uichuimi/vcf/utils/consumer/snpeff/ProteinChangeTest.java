package org.uichuimi.vcf.utils.consumer.snpeff;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uichuimi.vcf.io.VariantReader;
import org.uichuimi.vcf.utils.annotation.consumer.snpeff.ProteinChange;
import org.uichuimi.vcf.variant.Variant;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

class ProteinChangeTest {

	@Test
	void test() {

		final List<String> changes = List.of(
				// substitution
				"Trp24Cys", "(Trp24Cys)", "Trp24Ter", "Trp24*",
				"Cys188=", "0", "?", "Met1?", "(Gly56Ala^Ser^Cys)", "Trp24=/Cys",
				// deletion
				"Val7del", "(Val7del)", "Trp4del", "Lys23_Val25del", "(Pro458_Gly460del)",
				"Gly2_Met46del", "Trp26Ter", "Trp26*", "Val7=/del",
				// duplication
				"Ala3dup", "(Ala3dup)", "Ser6dup", "Ala3_Ser5dup",
				// insertion
				"His4_Gln5insAla", "Lys2_Gly3insGlnSerLys", "(Met3_His4insGlyTer)",
				"Arg78_Gly79ins23",
				// deletion/insertion
				"Cys28delinsTrpVal", "Cys28_Lys29delinsTrp", "(Pro578_Lys579delinsLeuTer)",
				"(Glu125_Ala132delinsGlyLeuHisArgPheIleValLeu)", "[Ser44Arg;Trp46Arg]",
				// two changes
//				"[Ser68Arg;Asn594del]", "[(Ser68Arg;Asn594del)]",
				// two alleles
//				"[Ser68Arg];[Ser68Arg]", "[(Ser68Arg)];[(Ser68Arg)]",
//				"(Ser68Arg)(;)(Ser68Arg)", "[Ser68Arg];[Asn594del]", "(Ser68Arg)(;)(Asn594del)",
//				"[(Ser68Arg)];[?]", "[Ser68Arg];[Ser68=]",
				// two proteins
//				"[Lys31Asn,Val25_Lys31del]",
				// mosaic
//				"[Arg49=/Ser]",
				// chimeric
//				"[Arg49=//Ser]",
				// repetition
				"Ala2[10]", "Ala2[10];[11]", "Gln18[23]",
//				"(Gln18)[(70_80)]",
				// frameshift
				"Arg97ProfsTer23", "Arg97fs", "(Tyr4*)", "Glu5ValfsTer5", "Glu5fs", "Ile327Argfs*?",
				"Ile327fs", "Gln151Thrfs*9");
		for (String change : changes) {
			try {
				final ProteinChange instance = ProteinChange.getInstance(change);
				if (instance == null) Assertions.fail(change);
				System.out.println(change + " / " + instance);
				System.out.println(instance.toVcfFormat());
			} catch (Exception e) {
				Assertions.fail(change, e);
			}
		}
	}

	@Test
	void testFromFile() {
		final File file = new File("/media/pascual/Resources/uichuimi/snpeff/variants.vcf.gz");
		final long start = System.nanoTime();
		try (VariantReader reader = new VariantReader(file)) {
			final long headers = reader.getHeader().toString().lines().count();
			long number = headers;
			for (Variant variant : reader) {
				number += 1;
				final List<String> ann = variant.getInfo("ANN");
				if (ann == null) continue;
				for (String element : ann) {
					final String[] values = element.split("\\|");
					if (values.length <= 10) continue;
					final String hgvsp = values[10];
					if (hgvsp.isBlank()) continue;
					if (!hgvsp.startsWith("p.")) {
						System.err.println(number + ": " + ann);
						break;
					}
					final ProteinChange change = ProteinChange.getInstance(hgvsp.substring("p.".length()));
					if (change == null) {
						System.err.println(number + ": " + ann);
						break;
					}
				}
			}
			final long elapsed = System.nanoTime() - start;
			System.out.printf("%d (%d)%n", number - headers, TimeUnit.NANOSECONDS.toSeconds(elapsed));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}