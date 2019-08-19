package org.uichuimi.vcf.utils.consumer.snpeff;

import org.uichuimi.vcf.utils.consumer.snpeff.grammar.AllelesToken;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProteinChange {

	private boolean predicted;
	private Range reference;
	private Collection<Amino> alternative;
	private boolean mosaic;
	private boolean chimeric;
	private Amino secondaryAlternative;
	private Integer position;
	private Type type;
	private Integer copies;

	static ProteinChange getInstance(String value) {
		final AllelesToken token = new AllelesToken();
		if (!token.consume(value)) return null;
		return token.getValue().getChanges().get(0);
		// substitution
		// (amino)(position)(amino)
		//  missense: Trp24Cys
		//  missense: (Trp24Cys)
		//  nonsense: Trp24Ter
		//  Trp24*
		//  silent  : Cys188=
		//  translation initiation codon: 0
		//  translation initiation codon: ?
		//  translation initiation codon: Met1?
		//  uncertain: (Gly56Ala^Ser^Cys)
		//  mosaic: Trp24=/Cys

		// deletion
		//  “amino_acid(s)+position(s)_deleted”“del”
		//  one amino acid: Val7del
		//  one amino acid: (Val7del)
		//  one amino acid: Trp4del
		//  several amino acids: Lys23_Val25del
		//  several amino acids: (Pro458_Gly460del)
		//  N-terminal: Gly2_Met46del
		//  C-terminal: Trp26Ter
		//  C-terminal: Trp26*
		//  mosaic: NP_003997.1: Val7=/del

		// duplication
		//  “amino_acid(s)+position(s)_duplicated”“dup”
		//  one amino acid: Ala3dup
		//  one amino acid: (Ala3dup)
		//  one amino acid: Ser6dup
		//  several amino acids: Ala3_Ser5dup

		// insertion
		//  “amino_acids+positions_flanking”“ins”“inserted_sequence”
		//	one amino acid: His4_Gln5insAla
		//  several amino acids: Lys2_Gly3insGlnSerLys
		//  several amino acids: (Met3_His4insGlyTer)
		//  repetitions: Arg78_Gly79ins23

		// deletion-insertion
		//  “amino_acid(s)+position(s)_deleted”“delins”“inserted_sequence”
		//  replacement: Cys28delinsTrpVal
		//  replacement: Cys28_Lys29delinsTrp
		//  replacement: (Pro578_Lys579delinsLeuTer)
		//  replacement: (Glu125_Ala132delinsGlyLeuHisArgPheIleValLeu)
		//  2 replacements: [Ser44Arg;Trp46Arg]

		// Alleles
		//  [“variant1”;”variant2”]
		//  variants on one allele: [Ser68Arg;Asn594del]
		//  variants on one allele: [(Ser68Arg;Asn594del)]
		//  homozygous: [Ser68Arg];[Ser68Arg]
		//  homozygous: [(Ser68Arg)];[(Ser68Arg)]
		//  homozygous: (Ser68Arg)(;)(Ser68Arg)
		//  heterozygous: [Ser68Arg];[Asn594del]
		//  heterozygous: (Ser68Arg)(;)(Asn594del)
		//  heterozygous: [(Ser68Arg)];[?]
		//  heterozygous: [Ser68Arg];[Ser68=]
		//  one allele encoding two proteins: [Lys31Asn,Val25_Lys31del]
		// somatic: [Arg49=/Ser]
		// mosaic: [Arg49=//Ser]

		// repeated
		//  “amino_acid(s)+position_repeat_unit””["”copy_number””]”
		//	Ala2[10]
		//  Ala2[10];[11]
		//	Gln18[23]
		//	(Gln18)[(70_80)]

		// frameshift
		//  “amino_acid”position”new_amino_acid”“fs”“Ter”“position_termination_site”
		//  Arg97ProfsTer23
		//  Arg97fs
		//  (Tyr4*)
		//  Glu5ValfsTer5
		//  Glu5fs
		//  Ile327Argfs*?
		//  Ile327fs
		//  Gln151Thrfs*9

		// Extension (N-terminal)
		//  “Met1”“ext”“position_new_initiation_site”

		// Extension (C-terminal)
		//  “Ter_position”“new_amino_acid”“ext”“position_new_termination_site”
	}

	public void setPredicted(boolean predicted) {
		this.predicted = predicted;
	}

	public void setReference(Range reference) {
		this.reference = reference;
	}

	public void setMosaic(boolean mosaic) {
		this.mosaic = mosaic;
	}

	public void setChimeric(boolean chimeric) {
		this.chimeric = chimeric;
	}

	public boolean getChimeric() {
		return chimeric;
	}

	public void setSecondaryAlternative(Amino secondaryAlternative) {
		this.secondaryAlternative = secondaryAlternative;
	}

	public Amino getSecondaryAlternative() {
		return secondaryAlternative;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public Integer getPosition() {
		return position;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public void setAlternative(Collection<Amino> alternative) {
		this.alternative = alternative;
	}

	public boolean isPredicted() {
		return predicted;
	}

	public Range getReference() {
		return reference;
	}

	public Collection<Amino> getAlternative() {
		return alternative;
	}

	public boolean isMosaic() {
		return mosaic;
	}

	public boolean isChimeric() {
		return chimeric;
	}

	public void setCopies(Integer copies) {
		this.copies = copies;
	}

	public Integer getCopies() {
		return copies;
	}

	@Override
	public String toString() {
		if (type.equals(Type.NO_PROTEIN)) return "0";
		if (type.equals(Type.UNKNOWN)) return "?";
		final StringBuilder rtn = new StringBuilder();
		if (predicted) rtn.append("(");
		// reference alternative
		rtn.append(reference);
		if (type.equals(Type.DELETION)) rtn.append("del");
		else if (type.equals(Type.DUPLICATION)) rtn.append("dup");
		else if (type.equals(Type.DELINS)) rtn.append("delins");
		else if (type.equals(Type.FRAMESHIFT)) rtn.append("fs");

		if (alternative != null) {
			if (alternative instanceof List) {
				rtn.append(alternative.stream().map(Amino::getShortName).collect(Collectors.joining()));
			} else if (alternative instanceof Set) {
				rtn.append(alternative.stream().map(Amino::getShortName).collect(Collectors.joining("^")));
			}
		}
		if (predicted) rtn.append(")");
		return rtn.toString();
	}

	public String toVcfFormat() {
		final String referenceSequence = reference == null ? null : getSequence(reference);
		switch (type) {
			case NO_PROTEIN:
				return "-";
			case UNKNOWN:
				return "?";
			case DUPLICATION:
				final String symbol = reference.getStartAmino().getSymbol();
				return symbol + "/" + symbol + symbol;
			case SUBSTITUTION: {
				Amino alt = alternative.iterator().next();
				if (alt == Amino.IDENTICAL) alt = reference.getStartAmino();
				return referenceSequence + "/" + alt.getSymbol();
			}
			case DELETION:
				return referenceSequence + "/-";
			case INSERTION: {
				String a = reference.getStartAmino().getSymbol();
				if (alternative == null) a += "X".repeat(copies);
				else
					a += alternative.stream().map(Amino::getSymbol).collect(Collectors.joining(""));
				a += reference.getEndAmino().getSymbol();
				return referenceSequence + "/" + a;
			}
			case DELINS: {
				String a = alternative.stream().map(Amino::getSymbol).collect(Collectors.joining());
				return referenceSequence + "/" + a;
			}
			case REPEATED: {
				return referenceSequence + "/" + referenceSequence.repeat(copies);
			}
			case FRAMESHIFT: {
				StringBuilder alt = new StringBuilder();
				if (alternative != null)
					alt.append(alternative.stream().map(Amino::getSymbol).collect(Collectors.joining()));
				else alt.append("X");
				if (position != null) {
					alt.append("X".repeat(position - 1));
					alt.append(Amino.TERMINATION.getSymbol());
				}
				return referenceSequence + "/" + alt;
			}
		}
		return "";
	}

	private String getSequence(Range range) {
		final StringBuilder rtn = new StringBuilder();
		rtn.append(range.getStartAmino().getSymbol());
		if (range.getEndAmino() != null) {
			rtn.append("X".repeat(range.getEndPosition() - range.getStartPosition() - 1));
			rtn.append(range.getEndAmino().getSymbol());
		}
		return rtn.toString();
	}

	public enum Type {
		DELINS, INSERTION, DUPLICATION, NO_PROTEIN, SUBSTITUTION, DELETION, FRAMESHIFT, REPEATED, UNKNOWN
	}
}
