#!/bin/sh
#
# Usage: curated-measure.sh {test,train}
#
# Note that this is a legacy script that is not maintained anymore;
# we use train-and-eval.sh for training models and benchmarking at once
# nowadays.  This script might get broken over time, you can repair it
# by comparing runtime options with _multistage_traineval.sh (which is
# the backend for train-and-eval.sh).

cid=$(git rev-parse --short HEAD)

type="$1"
args=
case $type in
	test) ;;
	train) args="-Dcz.brmlab.yodaqa.train_passextract=data/ml/tsv/training-passextract-${cid}.tsv -Dcz.brmlab.yodaqa.train_answer=data/ml/tsv/training-answer-${cid}.tsv -Dcz.brmlab.yodaqa.csv_answer=data/eval/answer-csv/${cid}";;
	*) echo "Usage: curated-measure.sh {test,train}" >&2; exit 1;;
esac

outfile="data/eval/tsv/curated-${type}-ovt-${cid}.tsv"
mkdir -p data/eval/tsv
mkdir -p data/eval/answer-csv

time ./gradlew check tsvgs \
	-PexecArgs="data/eval/curated-${type}.tsv $outfile" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa.analysis=debug \
	$args 2>&1 | tee logs/curated-${type}-$(git rev-parse --short HEAD).log
echo $outfile
