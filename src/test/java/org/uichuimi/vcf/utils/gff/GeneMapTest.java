package org.uichuimi.vcf.utils.gff;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uichuimi.vcf.variant.Coordinate;

import java.io.File;
import java.net.URL;

class GeneMapTest {

	private static GeneMap geneMap;

	@BeforeAll
	static void setUp() {
		final URL resource = GeneMapTest.class.getResource("/files/Homo_sapiens.GRCh38.95.gff3.gz");
		geneMap = new GeneMap(new File(resource.getFile()));
	}

	@Test
	void getGene() {
		final Coordinate coordinate = new Coordinate("19", 11113287);
		Assertions.assertEquals("LDLR", geneMap.getGene(coordinate).getName());
		Assertions.assertEquals("ENST00000545707", geneMap.getGene(coordinate).getTranscript(coordinate.getPosition()).getId());
	}


}