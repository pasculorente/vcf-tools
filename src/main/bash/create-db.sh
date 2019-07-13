#!/usr/bin/env bash

path="/media/pascual/Resources/uichuimi/neo4j/"
target="/var/lib/neo4j/data/databases/graph.db"

sudo rm -r "${target}"

sudo neo4j-admin import \
    --delimiter "\t" \
    --array-delimiter "," \
    --ignore-missing-nodes true \
    --nodes:DatabaseObject:Person ${path}Persons.tsv.gz \
    --nodes:DatabaseObject:Project ${path}Projects.tsv.gz \
    --nodes:DatabaseObject:Variant ${path}Variants.tsv.gz \
    --nodes:DatabaseObject:Gene ${path}Genes.tsv.gz \
    --nodes:DatabaseObject:Disease ${path}Diseases.tsv.gz \
    --nodes:DatabaseObject:Pathway ${path}Pathways.tsv.gz \
    --nodes:DatabaseObject:Frequency ${path}Frequencies.tsv.gz \
    --nodes:DatabaseObject:Effect ${path}Effects.tsv.gz \
    --nodes:DatabaseObject:Drug ${path}Drugs.tsv.gz \
    --relationships:INTERACTS_WITH ${path}interactions.tsv.gz \
    --relationships:HOMOZYGOUS ${path}homo.tsv.gz \
    --relationships:HETEROZYGOUS ${path}hetero.tsv.gz \
    --relationships:WILDTYPE ${path}wild.tsv.gz \
    --relationships:GENE ${path}var2gene.tsv.gz \
    --relationships:DISEASE ${path}gene2disease.tsv.gz \
    --relationships:PATHWAY ${path}gene2pathways.tsv.gz \
    --relationships:PROJECT ${path}sample2project.tsv.gz \
    --relationships:FREQUENCY ${path}var2freq.tsv.gz \
    --relationships:EFFECT ${path}var2effect.tsv.gz \
    --relationships:DRUG ${path}gene2drug.tsv.gz\
    --relationships ${path}variant2disease.tsv.gz

