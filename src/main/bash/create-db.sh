#!/usr/bin/env bash

path="/media/pascual/Resources/uichuimi/neo4j/"
target="/var/lib/neo4j/data/databases/graph.db"

sudo rm -r "${target}"

sudo neo4j-admin import \
    --delimiter "\t" \
    --array-delimiter "," \
    --ignore-missing-nodes true \
    --nodes:Person ${path}Persons.tsv \
    --nodes:Variant ${path}Variants.tsv \
    --nodes:Gene ${path}Genes.tsv \
    --nodes:Disease ${path}Diseases.tsv \
    --nodes:Pathway ${path}Pathways.tsv \
    --nodes:Frequency ${path}Frequencies.tsv \
    --relationships:INTERACTS_WITH ${path}interactions.tsv \
    --relationships:HOMOZYGOUS ${path}homo.tsv \
    --relationships:HETEROZYGOUS ${path}hetero.tsv \
    --relationships:WILDTYPE ${path}wild.tsv \
    --relationships:GENE ${path}var2gene.tsv \
    --relationships:DISEASE ${path}gene2disease.tsv \
    --relationships:PATHWAY ${path}gene2pathways.tsv \
    --relationships:PROJECT ${path}sample2project.tsv \
    --nodes:Project ${path}Projects.tsv \
    --relationships:FREQUENCY ${path}var2freq.tsv

