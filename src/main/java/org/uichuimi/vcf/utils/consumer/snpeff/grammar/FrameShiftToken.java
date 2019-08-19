package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.consumer.snpeff.Amino;
import org.uichuimi.vcf.utils.consumer.snpeff.ProteinChange;

import java.util.List;

public class FrameShiftToken implements Token<ProteinChange> {

	private static final String FS = "fs";
	private ProteinChange change;
	private String reminder;

	@Override
	public boolean consume(String value) {
		//fs|<AMINO>fs|<AMINO>fs<TER><POSITION>
		// short version, just the fs
		if (value.startsWith(FS)) {
			change = new ProteinChange();
			change.setType(ProteinChange.Type.FRAMESHIFT);
			reminder = value.substring(FS.length());
			return true;
		}
		// Long version, the new amino acid plus the position of the termination
		final AminoToken aminoToken = new AminoToken();
		if (!aminoToken.consume(value)) return false;
		value = aminoToken.getReminder();
		if (!value.startsWith(FS)) return false;
		change = new ProteinChange();
		change.setType(ProteinChange.Type.FRAMESHIFT);
		change.setAlternative(List.of(aminoToken.getValue()));
		value = value.substring(FS.length());
		reminder = value;
		// Do we have the position of the termination codon?
		if (value.startsWith(Amino.TERMINATION.getSymbol())) {
			value = value.substring(Amino.TERMINATION.getSymbol().length());
		} else if (value.startsWith(Amino.TERMINATION.getShortName())) {
			value = value.substring(Amino.TERMINATION.getShortName().length());
		} else return true;

		if (value.startsWith("?")) {
			// position of the termination codon is unknown
			reminder = value.substring("?".length());
			return true;
		}
		final PositionToken positionToken = new PositionToken();
		if (!positionToken.consume(value)) return false;
		change.setPosition(positionToken.getValue());
		reminder = positionToken.getReminder();
		return true;
	}

	@Override
	public String getReminder() {
		return reminder;
	}

	@Override
	public ProteinChange getValue() {
		return change;
	}
}
