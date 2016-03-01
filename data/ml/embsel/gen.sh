#!/bin/sh
#
# Usage: data/ml/embsel/gen.sh DATASET SENTSELDIR
#
# Example: data/ml/embsel/gen.sh moviesF ../Sentence-selection/

set -e

mkdir -p data/ml/embsel/propdata-${1}-train
./gradlew tsvgs -PexecArgs="data/eval/${1}-train.tsv ${1}-train.tsv" -Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug -Dcz.brmlab.yodaqa.dump_property_labels=data/ml/embsel/propdata-${1}-train 2>&1 | tee train_embsel.log

yodaqadir=$(pwd)
cd "$2"
./std_run.sh -p "$yodaqadir"/data/ml/embsel/propdata-${1}-train
mv data/Mbtemp.txt "$yodaqadir"/src/main/resources/cz/brmlab/yodaqa/analysis/rdf/Mbprop.txt
cd "$yodaqadir"

echo 'git commit -m"Mbprop.txt: Retrain on '"$1"'-train" src/main/resources/cz/brmlab/yodaqa/analysis/rdf/Mbprop.txt'
