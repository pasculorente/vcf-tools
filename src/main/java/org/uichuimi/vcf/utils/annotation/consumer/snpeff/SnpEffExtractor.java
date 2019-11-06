package org.uichuimi.vcf.utils.annotation.consumer.snpeff;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.uichuimi.vcf.header.VcfHeader;
import org.uichuimi.vcf.utils.annotation.consumer.VariantConsumer;
import org.uichuimi.vcf.utils.annotation.gff.Gene;
import org.uichuimi.vcf.utils.annotation.gff.GeneMap;
import org.uichuimi.vcf.variant.Variant;

import java.util.*;
import java.util.stream.Collectors;

import static org.uichuimi.vcf.utils.annotation.AnnotationConstants.*;

public class SnpEffExtractor implements VariantConsumer {

	private static final String ANN = "ANN";
	private static final String INTERGENIC_VARIANT = "intergenic_variant";
	private GeneMap geneMap;

	public SnpEffExtractor(GeneMap geneMap) {
		this.geneMap = geneMap;
	}

	@Override
	public void start(VcfHeader header) {
		if (!header.hasComplexHeader("INFO", ANN))
			System.err.println(" - WARNING: No ANN header line found");
	}

	@Override
	public void accept(Variant variant) {
		final List<String> ann = variant.getInfo(ANN);
		if (ann == null) return;
		// remove value from variant
		variant.setInfo(ANN, null);
		// http://snpeff.sourceforge.net/SnpEff_manual.html#inputº
		// Effect sort order. When multiple effects are reported, SnpEff sorts the effects the
		// following way:
		//
		// 1. Putative impact: Effects having higher putative impact are first.
		// 2. Effect type: Effects assumed to be more deleterious effects first.
		// 3. Canonical transcript before non-canonical.
		// 4. Marker genomic coordinates (e.g. genes starting before first).
		for (int i = 0; i < variant.getAlternatives().size(); i++) {
			final String allele = variant.getAlternatives().get(i);
			final Annotation annotation = ann.stream()
					.map(this::toAnnotation)
					.filter(it -> it.allele.equals(allele))
					.min(Comparator.comparingInt(s -> CONS_SEVERITY.indexOf(s.effects.get(0))))
					.orElse(null);
			if (annotation == null) return;
			setAlleleInfo(variant, i, ENSG, annotation.geneId);
			setAlleleInfo(variant, i, ENST, annotation.featureId);
			setAlleleInfo(variant, i, BIO, annotation.transcriptBiotype);
			setAlleleInfo(variant, i, FT, annotation.featureType);
			setAlleleInfo(variant, i, SYMBOL, annotation.symbol);
			setAlleleInfo(variant, i, CONS, annotation.effects.get(0));
			if (annotation.hgvsP != null && (!variant.getInfo().contains(AMINO)
					|| variant.<List<String>>getInfo(AMINO).size() <= i
					|| variant.<List<String>>getInfo(AMINO).get(i) == null)) {
				final ProteinChange change = ProteinChange.getInstance(annotation.hgvsP);
				if (change != null) setAlleleInfo(variant, i, AMINO, change.toVcfFormat());
			}
		}

	}

	private <T> void setAlleleInfo(Variant variant, int i, String key, T value) {
		if (value == null) return;
		List<T> field = variant.getInfo(key);
		if (field == null) {
			field = repeat(null, variant.getAlternatives().size());
			variant.setInfo(key, field);
		}
		field.set(i, value);
	}

	private Annotation toAnnotation(String line) {
		return new Annotation(line.split(ESCAPED_DELIMITER, -1));
	}

	@Override
	public void close() {
	}

	@NotNull
	private <T> List<T> repeat(T value, int n) {
		final List<T> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) list.add(value);
		return list;
	}

	/**
	 * Snpeff does not always contain proper ENSG in gene id
	 * @param name
	 * @return
	 */
	private String findGeneId(String name) {
		if (name.isBlank()) return null;
		if (name.startsWith("ENSG")) return name;
		// assume symbol
		if (name.contains("-")) {
			// for intergenic regions, there are two genes separated by -
			final String[] genes = name.split("-");
			for (String gene : genes) {
				final List<Gene> g = geneMap.getGeneBySymbol(gene);
				if (g != null) return g.get(0).getId();
			}
			return null;
		}
		final List<Gene> genes = geneMap.getGeneBySymbol(name);
		if (genes != null) return genes.get(0).getId();
		return null;
	}

	private class Annotation {

		private final Map<String, String> CONSEQUENCE_REPLACEMENTS = Map.of(
				"intergenic_region", INTERGENIC_VARIANT
		);

		static final String SECONDARY_SEPARATOR = "&";
		static final String TERTIARY_SEPARATOR = "/";
		private final String allele;
		private final List<String> effects;
		private final String impact;
		private final String symbol;
		private final String geneId;
		private final String featureType;
		private final String featureId;
		private final String transcriptBiotype;
		private final Integer rank;
		private final String hgvsC;
		private final String hgvsP;
		private final Integer cdnaPostion;
		private final Integer cdnaLength;
		private final Integer cdsPostion;
		private final Integer cdsLength;
		private final Integer proteinPostion;
		private final Integer proteinLength;
		private final Integer distance;
		private final List<String> logs;

		Annotation(String[] fields) {
			this.allele = fields[0];
			this.effects = fields[1].isBlank() ? Collections.emptyList() : collectAndReplaceEffects(fields[1]);
			this.impact = mapToNull(fields[2]);
			this.symbol = mapToNull(fields[3]);
			this.geneId = findGeneId(fields[4]);
			this.featureType = mapToNull(fields[5]);
			this.featureId = mapToNull(fields[6]);
			this.transcriptBiotype = mapToNull(fields[7]);
			// rank/total
			this.rank = fields[8].isBlank() ? null : Integer.valueOf(fields[8].split(TERTIARY_SEPARATOR)[0]);
			this.hgvsC = fields.length < 10 ? null : fields[9];
			this.hgvsP = fields.length < 11 || fields[10].isBlank() ? null : fields[10].substring(2);  // amino can be taken from here
			if (fields.length < 12 || fields[11].isBlank()) {
				this.cdnaPostion = this.cdnaLength = null;
			} else {
				final String[] elements = fields[11].split(TERTIARY_SEPARATOR);
				this.cdnaPostion = Integer.valueOf(elements[0]);
				this.cdnaLength = Integer.valueOf(elements[1]);
			}
			if (fields.length < 13 || fields[12].isBlank()) {
				this.cdsPostion = this.cdsLength = null;
			} else {
				final String[] elements = fields[12].split(TERTIARY_SEPARATOR);
				this.cdsPostion = Integer.valueOf(elements[0]);
				this.cdsLength = Integer.valueOf(elements[1]);
			}
			if (fields.length < 14 || fields[13].isBlank()) {
				this.proteinPostion = this.proteinLength = null;
			} else {
				final String[] elements = fields[13].split(TERTIARY_SEPARATOR);
				this.proteinPostion = Integer.valueOf(elements[0]);
				this.proteinLength = Integer.valueOf(elements[1]);
			}
			this.distance = ((fields.length < 15) || fields[14].isBlank()) ? null : Integer.valueOf(fields[14]);
			this.logs = fields.length < 16 || fields[15].isBlank()
					? Collections.emptyList()
					: Arrays.asList(fields[15].split(SECONDARY_SEPARATOR));
		}

		private List<String> collectAndReplaceEffects(String field) {
			return Arrays.stream(field.split(SECONDARY_SEPARATOR))
					.map(c -> CONSEQUENCE_REPLACEMENTS.getOrDefault(c, c))
					.collect(Collectors.toList());
		}

		@Nullable
		private String mapToNull(String field) {
			return field.isBlank() ? null : field;
		}

	}
}
