#!/bin/sh
# concepts_retrain.sh - Retrain the concepts classifier from scratch
#
# Usage: concepts_retrain.sh DATADIR
# Example: data/ml/concepts/concepts_retrain.sh ../dataset-factoid-movies/moviesF
#
# This is useful when the entity linking step of question analysis is modified,
# e.g. changed features or set of concepts.
#
# Requires dataset-factoid-movies checkout (with entity-linking.json gold
# standard) and Sentence-selection checkout.

set -e

datadir=$1

# First stage - training Mb concept matrix

./gradlew questionDump -PexecArgs="$datadir/train.json data/ml/concepts/questionDump-tofix.json" -Dcz.brmlab.yodaqa.topLinkedConcepts=0
python data/ml/repair-json.py data/ml/concepts/questionDump-tofix.json > data/ml/concepts/questionDump.json

rm -rf data/ml/embsel/concepts-moviesF-train
mkdir data/ml/embsel/concepts-moviesF-train
data/ml/concepts/concepts_embsel.py data/ml/concepts/questionDump.json "$datadir"/entity-linking.json data/ml/embsel/concepts-moviesF-train

basedir=$(pwd)
cd ../Sentence-selection/
./std_run.sh -p "$basedir"/data/ml/embsel/concepts-moviesF-train
cp data/Mbtemp.txt "$basedir"/src/main/resources/cz/brmlab/yodaqa/analysis/question/Mbdesc.txt
cd "$basedir"

# Second stage - training final concept classifier

./gradlew questionDump -PexecArgs="$datadir/train.json data/ml/concepts/questionDump-tofix.json" -Dcz.brmlab.yodaqa.topLinkedConcepts=0
python data/ml/repair-json.py data/ml/concepts/questionDump-tofix.json > data/ml/concepts/questionDump.json

echo "In src/main/java/cz/brmlab/yodaqa/analysis/question/ConceptClassifier.java paste the following:"
python data/ml/concepts/concepts_train_logistic.py data/ml/concepts/questionDump.json "$datadir/entity-linking.json"
echo "git commit src/main/java/cz/brmlab/yodaqa/analysis/question/ConceptClassifier.java src/main/resources/cz/brmlab/yodaqa/analysis/question/Mbdesc.txt -m\"ConceptClassifier: Retrain" # ...
