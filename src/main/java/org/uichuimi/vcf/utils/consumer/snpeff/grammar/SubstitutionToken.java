package org.uichuimi.vcf.utils.consumer.snpeff.grammar;

import org.uichuimi.vcf.utils.consumer.snpeff.ProteinChange;

public class SubstitutionToken implements Token<ProteinChange> {

	private static final String DEL = "del";
	private static final String SEPARATOR = "/";
	private ProteinChange change;
	private String reminder;

	@Override
	public boolean consume(String value) {
		// SET|SEQUENCE|SEQUENCE/AMINO|SEQUENCE//AMINO|SEQUENCE/del
		final SetToken setToken = new SetToken();
		if (setToken.consume(value)) {
			change = new ProteinChange();
			change.setType(ProteinChange.Type.SUBSTITUTION);
			change.setAlternative(setToken.getValue());
			reminder = setToken.getReminder();
			return true;
		}

		final SequenceToken sequenceToken = new SequenceToken();
		if (!sequenceToken.consume(value)) return false;
		value = sequenceToken.getReminder();
		change = new ProteinChange();
		change.setType(ProteinChange.Type.SUBSTITUTION);
		change.setAlternative(sequenceToken.getValue());
		reminder = value;
		if (!value.startsWith(SEPARATOR)) return true;
		value = value.substring(1);
		if (value.equals(DEL)) {
			change.setSecondaryAlternative(null);
			reminder = value.substring(DEL.length());
			return true;
		}
		boolean chimeric = false;
		boolean mosaic = false;
		if (value.startsWith(SEPARATOR)) {
			chimeric = true;
			value = value.substring(SEPARATOR.length());
		} else mosaic = true;
		final AminoToken secondary = new AminoToken();
		if (!secondary.consume(value)) return true;
		change.setSecondaryAlternative(secondary.getValue());
		change.setMosaic(mosaic);
		change.setChimeric(chimeric);
		reminder = secondary.getReminder();
		return true;
	}

	@Override
	public ProteinChange getValue() {
		return change;
	}

	@Override
	public String getReminder() {
		return reminder;
	}
}
