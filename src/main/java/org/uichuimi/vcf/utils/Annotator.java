package org.uichuimi.vcf.utils;

import org.uichuimi.variant.io.vcf.ComplexHeaderLine;
import org.uichuimi.variant.io.vcf.Variant;
import org.uichuimi.variant.io.vcf.VariantSet;
import org.uichuimi.variant.io.vcf.VcfHeader;
import org.uichuimi.variant.io.vcf.io.VariantSetReader;
import org.uichuimi.variant.io.vcf.io.VariantSetWriter;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;

public class Annotator {

	public static void main(String[] args) {
		final AnnotatorCli cli = new AnnotatorCli();
		new CommandLine(cli).parse(args);

		final VepReader vepReader = new VepReader(cli.getVep());
		final GeneReader genes = new GeneReader(cli.getGenesPath());

		final ProgressBar bar = new ProgressBar();
		try (VariantSetReader reader = new VariantSetReader(FileUtils.getInputStream(cli.getVcf())); VariantSetReader _1kgReader = new VariantSetReader(FileUtils.getInputStream(cli.get1000G())); VariantSetWriter writer = new VariantSetWriter(cli.getOutput())) {
			writer.setHeader(reader.header());

			addMissingHeaders(reader.header());
			int c = 0;
			while (reader.hasNext()) {
				final Variant variant = reader.next();
				final Variant vep = vepReader.getAnnotations(variant);
				final Variant kG = _1kgReader.next(variant.getCoordinate());
				if (vep != null) {
					if (variant.getId() == null || variant.getId().equals(VariantSet.EMPTY_VALUE))
						variant.setId(vep.getId());
					final String siftInfo = vep.getInfo().getString("Sift");
					if (siftInfo != null) {
						for (String siftField : siftInfo.split(",")) {
							final String[] siftValue = siftField.split("\\|");
							String sift = siftValue[1].replaceAll("_-_low_confidence", "");
							variant.getInfo().set("Sift", sift);
							break;
						}
					}
					final String polyphenInfo = vep.getInfo().getString("Polyphen");
					if (polyphenInfo != null) {
						for (String polyphenField : polyphenInfo.split(",")) {
							final String[] polyphenValue = polyphenField.split("\\|");
							variant.getInfo().set("Polyphen", polyphenValue[1]);
						}
					}

					final String varPep = vep.getInfo().getString("VarPep");
					if (varPep != null) {
						final String refPep = vep.getInfo().getString("RefPep");
						for (String varPepField : varPep.split(",")) {
							final String[] varPepValue = varPepField.split("\\|");
							String amino = varPepValue[1];
							variant.getInfo().set("AMINO", refPep + "/" + amino);
							break;
						}
					}

					final String csq = vep.getInfo().getString("CSQ");
					if (csq != null) {
						for (String consequenceField : csq.split(",")) {
							final String[] cons = consequenceField.split("\\|");
							// TODO: 28/02/19 insertion and deletion are indexed different
							if (cons[0].equals(variant.getAlt())) {
								variant.getInfo().set("CONS", cons[1]);
								break;
							}
						}
					}
//					final String ve = vep.getInfo().getString("VE");
//					if (ve != null) {
//						for (String veField : ve.split(",")) {
					//							final String[] veValue = veField.split("\\|");
//							variant.getInfo().set("BIO", veValue[0]);
//							break;
//						}
//					}

					if (kG != null) {
						for (String pop : Arrays.asList("EAS_AF", "SAS_AF", "EUR_AF", "AFR_AF", "AMR_AF")) {
							final Object[] value = kG.getInfo().getArray(pop);
							if (value == null) continue;
							final Double[] freqs = new Double[variant.getAltArray().length];
							for (int i = 0; i < variant.getAltArray().length; i++) {
								final String allele = variant.getAltArray()[i];
								final int index = indexOf(kG, allele);
								if (index < 0) freqs[i] = 0.0;
								else freqs[i] = (double) value[index];
							}
							if (Arrays.stream(freqs).anyMatch(Objects::nonNull)) {
								for (int i = 0; i < freqs.length; i++)
									if (freqs[i] == null) freqs[i] = 0.0;
								variant.getInfo().set(pop, freqs);
							}
						}
					}
					final GeneReader.Gene gene = genes.fromVariant(variant);
					if (gene != null) {
						variant.getInfo().set("SYMBOL", gene.getName());
						variant.getInfo().set("BIO", gene.getBiotype());
					}
					writer.write(variant);
				}
				if (c++ % 200 == 0)
					bar.update(GenomeProgress.getProgress(variant.getCoordinate()), variant.getCoordinate().toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Finds the position of the allele in the alt alleles of variant.
	 *
	 * @param variant target variant
	 * @param allele  allele to find in variant
	 * @return the index of the allele in variant in target.
	 */
	private static int indexOf(Variant variant, String allele) {
		final String[] alleles = variant.getAltArray();
		for (int i = 0; i < alleles.length; i++)
			if (alleles[i].equals(allele)) return i;
		return -1;
	}

	private static void addMissingHeaders(VcfHeader header) {
		int start = -1;
		int end = 1;
		for (int i = 0; i < header.getHeaderLines().size(); i++) {
			if (header.getHeaderLines().get(i) instanceof ComplexHeaderLine) {
				ComplexHeaderLine complexHeaderLine = (ComplexHeaderLine) header.getHeaderLines().get(i);
				if (complexHeaderLine.getKey().equals("INFO")) {
					if (start == -1) {
						start = i;
						end = i;
					} else end = i;
				}
			}
		}
		header.getHeaderLines().add(end, new ComplexHeaderLine("INFO", new LinkedHashMap<String, String>() {{
			put("ID", "AFR_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in African population (1000 Genomes Phase 3)");
		}}));
		header.getHeaderLines().add(end, new ComplexHeaderLine("INFO", new LinkedHashMap<String, String>() {{
			put("ID", "AMR_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in American population (1000 Genomes Phase 3)");
		}}));
		header.getHeaderLines().add(end, new ComplexHeaderLine("INFO", new LinkedHashMap<String, String>() {{
			put("ID", "EAS_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in East Asian population (1000 Genomes Phase 3)");
		}}));
		header.getHeaderLines().add(end, new ComplexHeaderLine("INFO", new LinkedHashMap<String, String>() {{
			put("ID", "EUR_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in European population (1000 Genomes Phase 3)");
		}}));
		header.getHeaderLines().add(end, new ComplexHeaderLine("INFO", new LinkedHashMap<String, String>() {{
			put("ID", "SAS_AF");
			put("Number", "A");
			put("Type", "Float");
			put("Description", "Allele frequency in South Asian population (1000 Genomes Phase 3)");
		}}));
		header.getHeaderLines().add(end, new ComplexHeaderLine("INFO", new LinkedHashMap<String, String>() {{
			put("ID", "Sift");
			put("Number", "1");
			put("Type", "String");
			put("Description", "Sift prediction");
		}}));
		header.getHeaderLines().add(end, new ComplexHeaderLine("INFO", new LinkedHashMap<String, String>() {{
			put("ID", "Polyphen");
			put("Number", "1");
			put("Type", "String");
			put("Description", "Polyphen prediction");
		}}));
		header.getHeaderLines().add(end, new ComplexHeaderLine("INFO", new LinkedHashMap<String, String>() {{
			put("ID", "CONS");
			put("Number", "1");
			put("Type", "String");
			put("Description", "Ensembl VEP consequence");
		}}));
		header.getHeaderLines().add(end, new ComplexHeaderLine("INFO", new LinkedHashMap<String, String>() {{
			put("ID", "BIO");
			put("Number", "1");
			put("Type", "String");
			put("Description", "Gene biotype");
		}}));
		header.getHeaderLines().add(end, new ComplexHeaderLine("INFO", new LinkedHashMap<String, String>() {{
			put("ID", "SYMBOL");
			put("Number", "1");
			put("Type", "String");
			put("Description", "Gene symbol");
		}}));
		header.getHeaderLines().add(end, new ComplexHeaderLine("INFO", new LinkedHashMap<String, String>() {{
			put("ID", "AMINO");
			put("Number", "1");
			put("Type", "String");
			put("Description", "Amino acid change (ref/alt)");
		}}));
	}
}
