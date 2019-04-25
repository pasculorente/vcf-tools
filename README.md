#VCF Utils

Another library to perform custom operations on VCF files

VCF is the standard file format to storage genetic variants. It's plain text, human readable, complex, 
redundant, inefficient and expensive.
However, as it has become one of the most used file formats in the bioinformatics community, it is necessary,
yet desirable, to process this files.

### How is the info stored in a VCF file?

VCF has 7 main fields (CHROM, POS, ID, REF, ALT, QUAL, FILTER) separated by tabs, following the good practise 
of CSV or TSV files.

When this 7 fields became too few, an extra field was added with key=value pairs separated by semicolons (;).
This field is called INFO. Every row must specify keys again and again.

Finally, two dimensional data is added at the end of each line. This data is called FORMAT. Both dimensions are
_keys_ and _samples_. To avoid too much redundancy keys are specified in a field separated by colons (:). Then
one column is added per sample, with the values corresponding to each key separated by colons as well.

But a line can store more than one genetic variant, so each field/info/format can be a list of values separated
by commas (,). This values can refer to alleles (R), alternative alleles (A), genotypes (G) or just an array (.) or an 
with _n_ values (1,2,3,4,...) as specified with the Number tag.

Below is an example of a _human readable_ line.
```text
chr1	26329479	rs34287969,rs1424016012,rs552129200,rs763292175	ATT,AT	A	569.73	.	AC=7;AF=0.875;AN=8;BaseQRankSum=-0.927;ClippingRankSum=0.809;DB;DP=82;ExcessHet=3.0103;FS=0;HaplotypeScore=.;InbreedingCoeff=.;MLEAC=1;MLEAF=0.5;MQ=54.35;MQRankSum=-0.78;QD=33.59;ReadPosRankSum=-0.937;KG_AFR_AF=0.6672;KG_AMR_AF=0.6023;KG_EAS_AF=0.5962;KG_EUR_AF=0.6392;KG_SAS_AF=0.6524;ENST=ENST00000527815;ENSG=ENSG00000176092;FT=primary_transcript;SYMBOL=CRYBG2;BIO=protein_coding;CONS=intron_variant;SOR=1.179	GT:AD:DP:GQ:PL	.:.:.:.	.:.:.:.	.:.:.:.	.:.:.:.	2/2:0,1:1:3:45,.,.,3,.,0	.:.:.:.	.:.:.:.	.:.:.:. .:.:.:.	.:.:.:.	.:.:.:.	.:.:.:.	.:.:.:.	.:.:.:.	.:.:.:.	.:.:.:.	2/2:0,1:1:3:45,.,.,3,.,0	.:.:.:.	.:.:.:.	2/2:0,1:1:3:41,.,.,3,.,0	.:.:.:.	1/2:41,38:79:99:.,.,607,.,0,666
```

VCF has two good point:

1. No escaping: Values between quotes are not permitted, so no need to escape things allowing an easy splitting.

2. Disambiguation: There is only one way to represent things, each line can be interpreted on its own (of course using 
the header).

### Data model
VCF file format represents multidimensional data. Multidimensional does not mean many fields, it means we are trying to 
fit a cube in a line.

First dimension is the variant itself. A line can store 1 or more variants in the same position. This dimension
can be unrolled by specifying each variant in a different line, even if they are in the same position.

Second dimension is the samples information. Not all samples have the same variants, but it tis mandatory to
specify the relationship in each line.

The only way to disassemble this dimension is by introducing a new table (so a new file) for the sample information.

There are also some hidden dimensions in the INFO field, like gene biotype, a value associated to genes, not variants.

A more accurate model would be:

Table|Data
---|---
Variant|position, 1 reference, 1 alternative, aminoacid change (consequence, depth, prediction)
Frequency|Population (eas, sas, eur), source (gnomad, 1000g), value
Gene|name, biotype
Called|how the sample called the variant (zygocity, depth)

Is it possible to store all of this information in a single file? Of course. There are standard data exchange formats 
like XML or JSON which allow to write this data with less ambiguity, although probably more space consuming and less 
_human readable_. But every single programming language has efficient and integrated parsers for both formats, there 
are international standards for both and data consistency can be strongly checked.

### Data processing
