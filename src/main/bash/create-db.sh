#!/usr/bin/env bash

files=$1
neo4j=$2
if [ $# == 2 ]; then
  target=$2
else
  target=$3
fi
echo "$files"
echo "$neo4j"
echo "$target"

rm -rf "${neo4j}data/databases/graph.db"

"${neo4j}"bin/neo4j-admin import \
    --delimiter "\t" \
    --array-delimiter "," \
    --ignore-missing-nodes true \
    --nodes:DatabaseObject:Person "${files}Persons.tsv.gz" \
    --nodes:DatabaseObject:Project "${files}Projects.tsv.gz" \
    --nodes:DatabaseObject:Variant "${files}Variants.tsv.gz" \
    --nodes:DatabaseObject:Gene "${files}Genes.tsv.gz" \
    --nodes:DatabaseObject:Disease "${files}Diseases.tsv.gz" \
    --nodes:DatabaseObject:Pathway "${files}Pathways.tsv.gz" \
    --nodes:DatabaseObject:Frequency "${files}Frequencies.tsv.gz" \
    --nodes:DatabaseObject:Effect "${files}Effects.tsv.gz" \
    --nodes:DatabaseObject:Drug "${files}Drugs.tsv.gz" \
    --nodes:DatabaseObject:Chromosome "${files}Chromosomes.tsv.gz" \
    --relationships:INTERACTS_WITH "${files}interactions.tsv.gz" \
    --relationships:HOMOZYGOUS "${files}homo.tsv.gz" \
    --relationships:HETEROZYGOUS "${files}hetero.tsv.gz" \
    --relationships:WILDTYPE "${files}wild.tsv.gz" \
    --relationships:GENE "${files}var2gene.tsv.gz" \
    --relationships:DISEASE "${files}gene2disease.tsv.gz" \
    --relationships:PATHWAY "${files}gene2pathways.tsv.gz" \
    --relationships:PROJECT "${files}sample2project.tsv.gz" \
    --relationships:FREQUENCY "${files}var2freq.tsv.gz" \
    --relationships:EFFECT "${files}var2effect.tsv.gz" \
    --relationships:DRUG "${files}gene2drug.tsv.gz" \
    --relationships "${files}variant2disease.tsv.gz" \
    --relationships:CHROMOSOME "${files}variant2chromosome.tsv.gz"

if [ "$target" != "$neo4j" ]; then
    echo "Moving data to ${target}data/databases/graph.db"
    rm -rf "${target}data/databases/graph.db"
    mv "${neo4j}data/databases/graph.db" "${target}data/databases/graph.db"
fi
echo "Done"
