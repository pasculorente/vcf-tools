package org.uichuimi.vcf.utils.consumer.vep;

import org.uichuimi.vcf.utils.consumer.vep.model.ColocatedVariant;
import org.uichuimi.vcf.utils.consumer.vep.model.IntergenicConsequence;
import org.uichuimi.vcf.utils.consumer.vep.model.TranscriptConsequence;
import org.uichuimi.vcf.utils.consumer.vep.model.VepResponse;
import org.uichuimi.vcf.variant.Variant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * VEP Created by uichuimi on 16/11/16.
 */
class VepVariantAnnotator {
	private static final List<String> CONS_SEVERITY = List.of(
			"transcript_ablation", "splice_acceptor_variant", "splice_donor_variant", "stop_gained",
			"frameshift_variant", "stop_lost", "start_lost", "transcript_amplification",
			"inframe_insertion", "inframe_deletion", "missense_variant", "protein_altering_variant",
			"splice_region_variant", "incomplete_terminal_codon_variant", "start_retained_variant",
			"stop_retained_variant", "synonymous_variant", "coding_sequence_variant",
			"mature_miRNA_variant", "5_prime_UTR_variant", "3_prime_UTR_variant",
			"non_coding_transcript_exon_variant", "intron_variant", "NMD_transcript_variant",
			"non_coding_transcript_variant", "upstream_gene_variant", "downstream_gene_variant",
			"TFBS_ablation", "TFBS_amplification", "TF_binding_site_variant",
			"regulatory_region_ablation", "regulatory_region_amplification", "feature_elongation",
			"regulatory_region_variant", "feature_truncation", "intergenic_variant"
	);

	static void annotate(Variant variant, Collection<VepResponse> annotations) {
		for (int i = 0; i < variant.getAlternatives().size(); i++) {
			for (VepResponse annotation : annotations) {
				addColocatedVariants(variant, i, annotation.getColocatedVariants());
				addTranscriptConsequences(variant, i, annotation.getTranscriptConsequences());
				addIntergenicConsequences(variant, i, annotation.getIntergenicConsequences());
			}

		}
	}

	private static void addColocatedVariants(Variant variant, int allele, Collection<ColocatedVariant> variants) {
		if (variants == null) return;
		final ColocatedVariant var = variants.stream()
				.findFirst().orElse(null);
		if (var == null) return;

		// ID goes in the VCF id field
		String id = var.getId();
		if (!variant.getIdentifiers().contains(id)) variant.getIdentifiers().add(id);

		putIfNotNull(variant, allele, var.getMinorAlleleFreq(), "GMAF");

		putIfNotNull(variant, allele, var.getAmrMaf(), "KG_AMR_AF");
		putIfNotNull(variant, allele, var.getEurMaf(), "KG_EUR_AF");
		putIfNotNull(variant, allele, var.getEasMaf(), "KG_EAS_AF");
		putIfNotNull(variant, allele, var.getSasMaf(), "KG_SAS_AF");
		putIfNotNull(variant, allele, var.getAfrMaf(), "KG_AFR_AF");
		putIfNotNull(variant, allele, var.getEaMaf(), "KG_EA_AF");
		putIfNotNull(variant, allele, var.getAaMaf(), "KG_AA_AF");

		putIfNotNull(variant, allele, var.getExacSasMaf(), "EX_SAS_AF");
		putIfNotNull(variant, allele, var.getExacAfrMaf(), "EX_AFR_AF");
		putIfNotNull(variant, allele, var.getExacEasMaf(), "EX_EAS_AF");
		putIfNotNull(variant, allele, var.getExacFinMaf(), "EX_FIN_AF");
		putIfNotNull(variant, allele, var.getExacOthMaf(), "EX_OTH_AF");
		putIfNotNull(variant, allele, var.getExacAdjMaf(), "EX_ADJ_AF");
		putIfNotNull(variant, allele, var.getExacNfeMaf(), "EX_NFE_AF");
		putIfNotNull(variant, allele, var.getExacAmrMaf(), "EX_AMR_AF");
	}

	private static void addTranscriptConsequences(Variant variant, int allele, Collection<TranscriptConsequence> consequences) {
		if (consequences == null) return;
		// Only take the most severe
		final TranscriptConsequence consequence = consequences.stream()
				.filter(cons -> cons.getVariantAllele().equals(variant.getAlternatives().get(allele)))
				.min(Comparator.comparingInt(c -> (CONS_SEVERITY.indexOf(c.getConsequenceTerms().iterator().next()))))
				.orElse(null);
		if (consequence == null) return;
		putIfNotNull(variant, allele, consequence.getGeneSymbol(), "SYMBOL");
		putIfNotNull(variant, allele, consequence.getGeneId(), "ENSG");
		putIfNotNull(variant, allele, consequence.getBiotype(), "BIO");
		putIfNotNull(variant, allele, consequence.getTranscriptId(), "ENST");
		putIfNotNull(variant, allele, consequence.getAminoAcids(), "AMINO");

		putIfNotNull(variant, allele, consequence.getSiftPrediction(), "Sift");
		putIfNotNull(variant, allele, consequence.getPolyphenPrediction(), "Polyphen");
		putIfNotNull(variant, allele, consequence.getConsequenceTerms().iterator().next(), "CONS");

	}

	private static <T> void putIfNotNull(Variant variant, int allele, T value, String key) {
		if (value == null) return;
		List<T> list = variant.getInfo(key);
		if (list == null) list = new ArrayList<>();
		while (list.size() <= allele) list.add(null);
		list.set(allele, value);
		variant.setInfo(key, list);
	}

	private static void addIntergenicConsequences(Variant variant, int allele, Collection<IntergenicConsequence> consequences) {
		if (consequences == null) return;
		final IntergenicConsequence consequence = consequences.stream()
				.filter(cons -> cons.getVariantAllele().equals(variant.getAlternatives().get(allele)))
				.findFirst()
				.orElse(null);
		if (consequence == null) return;
		putIfNotNull(variant, allele, consequence.getConsequenceTerms().iterator().next(), "CONS");
	}


}
