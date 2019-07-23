package org.uichuimi.vcf.utils.consumer.vep;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uichuimi.vcf.utils.common.CoordinateUtils;
import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.util.List;

class EnsemblRestTest {

	@Test
	public void annotate() {
		final Variant variant = new Variant(null, new Coordinate("chr19", 11113287), List.of("CCTACCTCTT"), List.of("C"));
		final EnsemblRest rest = new EnsemblRest("http://rest.ensembl.org/");
		rest.annotate(variant, CoordinateUtils.toGrch38(variant.getCoordinate()));
		Assertions.assertEquals(List.of("AYLF/A"), variant.getInfo().get("AMINO"));
		Assertions.assertEquals(List.of("ENSG00000130164"), variant.getInfo().get("ENSG"));
		Assertions.assertEquals(List.of("ENST00000252444"), variant.getInfo().get("ENST"));
		Assertions.assertEquals(List.of("LDLR"), variant.getInfo().get("SYMBOL"));
		Assertions.assertNull(variant.getInfo().get("Sift"));
		Assertions.assertNull(variant.getInfo().get("Polyphen"));
		Assertions.assertEquals(List.of("inframe_deletion"), variant.getInfo().get("CONS"));
		Assertions.assertEquals(List.of("protein_coding"), variant.getInfo().get("BIO"));
		Assertions.assertNull(variant.getInfo().get("FT"));
	}

}