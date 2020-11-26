package org.uichuimi.vcf.utils.annotation;

public class ColumnSpec {
	private final String sourceColumn;
	private final String targetColumn;

	public ColumnSpec(String sourceColumn, String targetColumn) {
		this.sourceColumn = sourceColumn;
		this.targetColumn = targetColumn;
	}

	public String getSourceColumn() {
		return sourceColumn;
	}

	public String getTargetColumn() {
		return targetColumn;
	}

	@Override
	public String toString() {
		return "ColumnSpec{" +
				"sourceColumn='" + sourceColumn + '\'' +
				", targetColumn='" + targetColumn + '\'' +
				'}';
	}
}
