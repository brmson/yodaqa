#!/bin/sh
#
# Usage: _multistage-traineval.sh BASEDIR {train,test}
#
# In training mode, run the pipeline for all questions up to the
# answer scoring, stop there, perform training, update the model
# and re-run just the answer scoring to get the final results.
#
# Testing mode (evaluation on test set) is designed to run in
# parallel with the training mode and works in lock-step, stopping
# before scoring to wait and use the new model.
#
# All data and logs are stored relative to BASEDIR.  The project
# is assumed to be built already (using `mvn verify`).  This is
# meant to be used via the data/eval/train-and-eval.sh script.

## Setup and initialize

cid=$(git rev-parse --short HEAD)

basedir="$1"
type="$2"
args0=
argsF=

outfile0="$basedir/data/eval/tsv/curated-${type}-ovt-u${cid}.tsv"
outfileF="$basedir/data/eval/tsv/curated-${type}-ovt-${cid}.tsv"
atrainfile="$basedir/data/ml/tsv/training-answer-${cid}.tsv"
modelfile="$basedir/data/ml/models/logistic-${cid}.model"
xmidir="$basedir/data/eval/answer-xmi/${cid}-$type"
barrierfile=_multistage-barrier
mkdir -p $basedir/data/eval/tsv
mkdir -p $basedir/data/eval/answer-csv
mkdir -p $basedir/data/ml/models
mkdir -p "$xmidir"

case $type in
	test) ;;
	train) args0="-Dcz.brmlab.yodaqa.train_passextract=$basedir/data/ml/tsv/training-passextract-${cid}.tsv -Dcz.brmlab.yodaqa.train_answer=$atrainfile -Dcz.brmlab.yodaqa.csv_answer=$basedir/data/eval/answer-csv/${cid}";;
	*) echo "Usage: $0 BASEDIR {test,train}; but use the train-and-eval wrapper." >&2; exit 1;;
esac

if [ -e $barrierfile ]; then
	echo "$barrierfile: Already exists" >&2
	read
	exit 1
fi

## Run the pipelien

{

## Gather answers once, also storing the answerfvs
echo "First run..."
time mvn exec:java -Ptsvgs \
	-Dexec.args="$basedir/data/eval/curated-${type}.tsv $outfile0" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa.analysis=debug \
	-Dcz.brmlab.yodaqa.save_answerfvs="$xmidir" \
	$args0

## Train the model
if [ "$type" = "train" ]; then
	echo "Training..."
	data/ml/answer-train.py <"$atrainfile" | tee "$modelfile"
	cp "$modelfile" src/main/resources/cz/brmlab/yodaqa/analysis/answer/AnswerScoreLogistic.model
	echo "Rebuilding with new model..."
	mvn verify
	touch "$barrierfile" # testing is go, too!

else  # test
	# Just wait for the training to finish; XXX ugly this way
	echo "Waiting for $barrierfile"
	while [ ! -e "$barrierfile" ]; do
		echo -n .
		sleep 2
	done
	echo " :)"
	rm "$barrierfile"
fi

time mvn exec:java -Ptsvgs \
	-Dexec.args="$basedir/data/eval/curated-${type}.tsv $outfileF" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa.analysis=debug \
	-Dcz.brmlab.yodaqa.load_answerfvs="$xmidir" \
	$argsF

echo "$outfileF"

} 2>&1 | tee "$basedir/logs/curated-${type}-${cid}.log"
