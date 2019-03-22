package org.uichuimi.vcf.utils;

import org.uichuimi.variant.io.vcf.Variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class has 2 tasks:
 *
 * <ol>
 * <li>Create genes table</li>
 * <li>Index gene identifier to map from different resources</li>
 * </ol>
 * The genes table contains 3 columns: <b>id</b>, <b>name</b> and <b>biotype</b>. <em>id</em> is
 * taken from Ensebml. That means that resources using uniprot, Entrez Gene or HGNC identifiers will
 * not be linked properly. To this, links from un
 */
public class GeneReader {

	private Map<String, List<Gene>> chromosomes = new HashMap<>();


	public GeneReader(File path) {
		readGenes(path);
	}

	private void readGenes(File path) {
		try (final BufferedReader geneReader = FileUtils.getBufferedReader(path)) {
			String line;
			while ((line = geneReader.readLine()) != null) {
				final Feature feature = parse(line);
				if (feature instanceof Gene) {
					final Gene gene = (Gene) feature;
					chromosomes.computeIfAbsent(gene.chromosome, c -> new ArrayList<>()).add(gene);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		for (List<Gene> list : chromosomes.values()) list.sort(Gene::compareTo);
	}

	private Feature parse(String line) {
		if (line.startsWith("#")) return null;
		final String[] row = line.split("\t");
		//   seqid     - The ID of the landmark used to establish the coordinate system for the current
		//               feature. IDs may contain any characters, but must escape any characters not in
		//               the set [a-zA-Z0-9.:^*!+_?-|]. In particular, IDs may not contain unescaped
		//               whitespace and must not begin with an unescaped ">".
		final String chrom = row[0];
		//   source    - The source is a free text qualifier intended to describe the algorithm or
		//               operating procedure that generated this feature. Typically this is the name of a
		//               piece of software, such as "Genescan" or a database name, such as "Genbank." In
		//               effect, the source is used to extend the feature ontology by adding a qualifier
		//               to the type creating a new composite type that is a subclass of the type in the
		//               type column.
		//   type      - The type of the feature (previously called the "method"). This is constrained to
		//               be either: (a)a term from the "lite" version of the Sequence Ontology - SOFA, a
		//               term from the full Sequence Ontology - it must be an is_a child of
		//               sequence_feature (SO:0000110) or (c) a SOFA or SO accession number. The latter
		//               alternative is distinguished using the syntax SO:000000.
		// bidirectional_promoter_lncRNA, biological_region, CDS, chromosome, exon, five_prime_UTR, gene, lnc_RNA,
		// miRNA, mRNA, ncRNA, ncRNA_gene, pseudogene, pseudogenic_transcript, rRNA, scRNA, snoRNA, snRNA,
		// three_prime_overlapping_ncrna, three_prime_UTR, transcript, unconfirmed_transcript
		// final String type = row[2];

		//   start     - start position of the feature in positive 1-based integer coordinates
		//               always less than or equal to end
		final Integer start = Integer.valueOf(row[3]);
		//   end       - end position of the feature in positive 1-based integer coordinates
		final Integer end = Integer.valueOf(row[4]);
		//   score     - The score of the feature, a floating point number. As in earlier versions of the
		//               format, the semantics of the score are ill-defined. It is strongly recommended
		//               that E-values be used for sequence similarity features, and that P-values be
		//               used for ab initio gene prediction features.
		//   strand    - The strand of the feature. + for positive strand (relative to the landmark), -
		//               for minus strand, and . for features that are not stranded. In addition, ? can
		//               be used for features whose strandedness is relevant, but unknown.
		//   phase     - For features of type "CDS", the phase indicates where the feature begins with
		//               reference to the reading frame. The phase is one of the integers 0, 1, or 2,
		//               indicating the number of bases that should be removed from the beginning of this
		//               feature to reach the first base of the next codon. In other words, a phase of
		//               "0" indicates that the next codon begins at the first base of the region
		//               described by the current line, a phase of "1" indicates that the next codon
		//               begins at the second base of this region, and a phase of "2" indicates that the
		//               codon begins at the third base of this region. This is NOT to be confused with
		//               the frame, which is simply start modulo 3.
		//   attribute - A list of feature attributes in the format tag=value. Multiple tag=value pairs
		//               are separated by semicolons. URL escaping rules are used for tags or values
		//               containing the following characters: ",=;". Spaces are allowed in this field,
		//               but tabs must be replaced with the %09 URL escape. Attribute values do not need
		//               to be and should not be quoted. The quotes should be included as part of the
		//               value by parsers and not stripped.
		final String[] attributes = row[8].split(";");
		String name = null;
		String biotype = null;
		String id = null;
		String typ = null;
		String parent = null;
		for (String attribute : attributes) {
			final String[] keyValue = attribute.split("=");
			final String key = keyValue[0];
			final String value = keyValue[1];
			switch (key) {
				case "Name":
					name = value;
					break;
				case "biotype":
					biotype = value;
					break;
				case "Parent": {
					final String[] split = value.split(":");
					parent = split[1];
					break;
				}
				case "ID": {
					// ID=gene:ENSG00001234
					final String[] split = value.split(":");
					typ = split[0];
					id = split[1];
					break;
				}
			}
		}
		if (typ == null) return null;
		if (typ.equals("gene")) return new Gene(id, name, biotype, chrom, start, end);
		else if (typ.equals("transcript")) return new Transcript(id, parent);
		return null;
	}

	public Gene fromVariant(Variant variant) {
		final List<Gene> genes = chromosomes.get(variant.getChrom());
		if (genes == null) return null;
		final Gene gene = binarySearch(genes, variant.getPosition());
		return gene;
	}

	// Adapted from Collections#binarySearch
	private Gene binarySearch(List<Gene> list, int position) {
		int low = 0;
		int high = list.size() - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			final Gene gene = list.get(mid);
			int cmp = gene.compareTo(position);

			if (cmp < 0) low = mid + 1;
			else if (cmp > 0) high = mid - 1;
			else return gene; // key found
		}
		return null;
	}


	private class Feature {
	}

	private class Transcript extends Feature {

		private String id;
		private String geneId;

		Transcript(String id, String geneId) {
			this.id = id;
			this.geneId = geneId;
		}
	}

	class Gene extends Feature implements Comparable<Gene> {
		private final String chromosome;
		private final String id;
		private final String name;
		private final String biotype;
		private final Integer start;
		private final Integer end;

		Gene(String id, String name, String biotype, String chromosome, Integer start, Integer end) {
			this.id = id;
			this.name = name;
			this.biotype = biotype;
			this.chromosome = chromosome;
			this.start = start;
			this.end = end;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getBiotype() {
			return biotype;
		}

		public Integer getEnd() {
			return end;
		}

		public Integer getStart() {
			return start;
		}

		public String getChromosome() {
			return chromosome;
		}

		@Override
		public int compareTo(Gene other) {
			if (other == this) return 0;
			final int compare = Integer.compare(start, other.start);
			if (compare != 0) return compare;
			return Integer.compare(end, other.end);
		}

		/**
		 * Compares the current gene with the given position, returning the relative position of the
		 * gene with respect to the position. If the gene has <code>start <= end < position</code>,
		 * the result is < 0, if <code>position < start <= end</code>, result is > 0. If
		 * <code>start<= position <= end</code> returns 0.
		 */
		int compareTo(int position) {
			if (position < start) return 1;
			if (end < position) return -1;
			return 0;
		}
	}


}
