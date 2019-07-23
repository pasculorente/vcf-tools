package org.uichuimi.vcf.utils.consumer.vep;

import org.uichuimi.vcf.variant.Coordinate;
import org.uichuimi.vcf.variant.Variant;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;

public class EnsemblRest {

	private final Queue<Variant> variants = new ArrayDeque<>(20);
	private final VepWebConnection connection;

	public EnsemblRest(String url) {
		connection = new VepWebConnection(url);
	}

	public void annotate(Variant variant, Coordinate coordinate) {
		VepVariantAnnotator.annotate(variant, connection.getAnnotations(Collections.singleton(variant)));
	}
}
