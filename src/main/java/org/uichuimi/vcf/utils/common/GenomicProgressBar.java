package org.uichuimi.vcf.utils.common;

import org.uichuimi.vcf.variant.Coordinate;

public class GenomicProgressBar extends ProgressBar {

	public void update(Coordinate coordinate, String message) {
		super.update(GenomeProgress.getProgress(CoordinateUtils.toGrch38(coordinate)), message);
	}
}
