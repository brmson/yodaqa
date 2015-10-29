#!/bin/sh
# concepts_retrain.sh - Retrain the concepts classifier from scratch
#
# Usage: concepts_retrain.sh DATADIR
# Example: data/ml/fbpath/concepts_retrain.sh ../dataset-factoid-movies/moviesC
#
# This is useful when the entity linking step of question analysis is modified,
# e.g. changed features or set of concepts.
#
# Requires dataset-factoid-movies checkout (with entity-linking.json gold
# standard).

set -e

datadir=$1

./gradlew questionDump -PexecArgs="$datadir/train.json data/ml/concepts/questionDump-tofix.json" -Dcz.brmlab.yodaqa.topLinkedConcepts=0
python data/ml/repair-json.py data/ml/concepts/questionDump-tofix.json > data/ml/concepts/questionDump.json

echo "In src/main/java/cz/brmlab/yodaqa/analysis/question/ConceptClassifier.java paste the following:"
python data/ml/concepts/concepts_train_logistic.py data/ml/concepts/questionDump.json "$datadir/entity-linking.json"
echo "git commit src/main/java/cz/brmlab/yodaqa/analysis/question/ConceptClassifier.java -m\"ConceptClassifier: Retrain" # ...
