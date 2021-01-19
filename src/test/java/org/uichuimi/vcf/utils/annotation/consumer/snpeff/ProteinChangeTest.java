package org.uichuimi.vcf.utils.annotation.consumer.snpeff;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

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
		int x = 7;
	}

}
