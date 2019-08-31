package org.uichuimi.vcf.utils.common;

import org.uichuimi.vcf.variant.Coordinate;

import java.io.PrintStream;

public class GenomicProgressBar extends ProgressBar {

	public GenomicProgressBar() {
	}

	public GenomicProgressBar(PrintStream out) {
		super(out);
	}

	public void update(Coordinate coordinate, String message) {
		super.update(GenomeProgress.getProgress(CoordinateUtils.toGrch38(coordinate)), message);
	}
}
