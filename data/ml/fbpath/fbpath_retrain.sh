#!/bin/sh
# fbpath_retrain.sh - Retrain the fbpath classifier from scratch
#
# Usage: fbpath_retrain.sh DATASET_CHECKOUT [GOOGLE_API_KEY]
# Example: data/ml/fbpath/fbpath_retrain.sh ../dataset-factoid-webquestions $googleapikey
#
# This is useful when question analysis is modified (e.g. entity linking).
# Requires dataset-factoid-webquestions checkout.
#
# !!! You may want to run scripts/dump-refresh.sh in there first !!!

set -e

dataset_co=$1
googleapikey=$2
basedir=$(pwd)

mkdir -p data/ml/fbpath/wq-fbpath

cd "$dataset_co"
for i in trainmodel val devtest; do
	scripts/fulldata.py $i "$basedir/data/ml/fbpath/wq-fbpath/" main/ d-dump/ d-freebase-brp/
done
cd "$basedir"

data/ml/fbpath/fbpath_train_logistic.py data/ml/fbpath/wq-fbpath/trainmodel.json data/ml/fbpath/wq-fbpath/val.json >src/main/resources/cz/brmlab/yodaqa/analysis/rdf/FBPathLogistic.model

echo "git commit src/main/resources/cz/brmlab/yodaqa/analysis/rdf/FBPathLogistic.model -m\"FBPathLogistic: Retrain using "
