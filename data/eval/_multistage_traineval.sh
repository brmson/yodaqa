#!/bin/bash
#
# Usage: _multistage-traineval.sh BASEDIR DATASET RETRAIN WAIT_FOR_TRAIN [BASECOMMIT]
#
# Run the pipeline for all questions in DATASET, but pause and
# restart the pipeline at each train/test checkpoint.  If RETRAIN
# is 1, retrain the scoring models; if WAIT_FOR_TRAIN is 1,
# we expect that a RETRAIN evaluation is running in parallel
# and we wait on each checkpoint for the new model.
#
# Typically, this script is run on training dataset with
# RETRAIN 1, WAIT_FOR_TRAIN 0, and on testing dataset with
# RETRAIN 0, WAIT_FOR_TRAIN 1.  But if we want to do additional
# evaluation on another dataset, we may invoke it as RETRAIN 0,
# WAIT_FOR_TRAIN 0.
#
# If BASECOMMIT is specified, the pipeline phase 0 is not run
# but instead data files are symlinked from BASECOMMIT evals.
#
# All data and logs are stored relative to BASEDIR.  The project
# is assumed to be built already (using `./gradlew check`).  This is
# meant to be used via the data/eval/train-and-eval.sh script.

## Setup and initialize

cid=$(git rev-parse --short HEAD)

basedir="$1"
dataset="$2"
retrain="$3"
wait_for_train="$4"
basecommit="$5"
system_property="$6"
args0=
argsF=

outfile0="$basedir/data/eval/tsv/${dataset}-ovt-u${cid}.tsv"
outfile1="$basedir/data/eval/tsv/${dataset}-ovt-v${cid}.tsv"
outfile2="$basedir/data/eval/tsv/${dataset}-ovt-${cid}.tsv"
atrainfile0="$basedir/data/ml/tsv/${dataset}-answer-${cid}.tsv"
atrainfile1="$basedir/data/ml/tsv/${dataset}-answer1-${cid}.tsv"
atrainfile2="$basedir/data/ml/tsv/${dataset}-answer2-${cid}.tsv"
modelfile0="$basedir/data/ml/models/decision-forest-${cid}.model"
modelfile1="$basedir/data/ml/models/decision-forest1-${cid}.model"
modelfile2="$basedir/data/ml/models/decision-forest2-${cid}.model"
xmidir="$basedir/data/eval/answer-xmi/${cid}-$dataset"
csvdir="$basedir/data/eval/answer-csv/${cid}-$dataset"
csv1dir="$basedir/data/eval/answer1-csv/${cid}-$dataset"
csv2dir="$basedir/data/eval/answer2-csv/${cid}-$dataset"
barrierfile=_multistage-barrier
mkdir -p $basedir/data/eval/tsv
mkdir -p $basedir/data/eval/answer-csv
mkdir -p $basedir/data/eval/answer1-csv
mkdir -p $basedir/data/eval/answer2-csv
mkdir -p $basedir/data/ml/models
mkdir -p "$xmidir" "$xmidir"1 "$xmidir"2

if [ "$retrain" = 1 ]; then
	args0="-Dcz.brmlab.yodaqa.train_passextract=$basedir/data/ml/tsv/${dataset}-passextract-${cid}.tsv
	       -Dcz.brmlab.yodaqa.train_answer=$atrainfile0 -Dcz.brmlab.yodaqa.csv_answer=$csvdir"
	args1="-Dcz.brmlab.yodaqa.train_answer1=$atrainfile1 -Dcz.brmlab.yodaqa.csv_answer1=$csvdir1"
	args2="-Dcz.brmlab.yodaqa.train_answer2=$atrainfile2 -Dcz.brmlab.yodaqa.csv_answer2=$csvdir2"
fi

if [ -e $barrierfile -o -e ${barrierfile}1 -o -e ${barrierfile}2 ]; then
	echo "$barrierfile: Already exists" >&2
	read x
	exit 1
fi

train_and_sync() {
	i=$1
	atrainfile=$2
	modelfile=$3

	## Train the model
	if [ "$retrain" = "1" ]; then
		echo "Training ${i}..."
		data/ml/answer-train-gradboost.py <"$atrainfile" | tee "$modelfile"
		cp "$modelfile" src/main/resources/cz/brmlab/yodaqa/analysis/ansscore/AnswerScoreDecisionForest${i}.model
		echo "Rebuilding with new model..."
		./gradlew check
		touch "$barrierfile$i" # testing is go, too!

	elif [ "$wait_for_train" = "1" ]; then  # test
		# Just wait for the training to finish; XXX ugly this way
		echo "Waiting for $barrierfile$i, #${i}"
		while [ ! -e "$barrierfile$i" ]; do
			echo -n .
			sleep 2
		done
		echo " :)"
		rm "$barrierfile$i"
	fi
}

## Run the pipeline

{

if [ -z "$basecommit" ]; then
	## Gather answers once, also storing the answerfvs
	echo "First run..."
	time ./gradlew tsvgs \
		-PexecArgs="$basedir/data/eval/${dataset}.tsv $outfile0" \
		-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug \
		-Dcz.brmlab.yodaqa.save_answerfvs="$xmidir" \
		$system_property \
		$args0
	base_xmidir="$xmidir"
	base_atrainfile0="$atrainfile0"

else
	## Reuse data files from $basecommit
	echo "Reusing phase0 data from ${basecommit}"
	base_xmidir="$basedir/data/eval/answer-xmi/${basecommit}-$dataset"
	gunzip "$base_xmidir"/*.gz || :
	base_atrainfile0="$basedir/data/ml/tsv/${dataset}-answer-${basecommit}.tsv"
fi

train_and_sync "" "$base_atrainfile0" "$modelfile0"

# Re-score with new model
time ./gradlew tsvgs \
	-PexecArgs="$basedir/data/eval/${dataset}.tsv $outfile0" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug \
	-Dcz.brmlab.yodaqa.load_answerfvs="$base_xmidir" \
	-Dcz.brmlab.yodaqa.save_answerfvs="$xmidir" \
	$system_property \
	$args0


time ./gradlew tsvgs \
	-PexecArgs="$basedir/data/eval/${dataset}.tsv $outfile1" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug \
	-Dcz.brmlab.yodaqa.load_answerfvs="$xmidir" \
	-Dcz.brmlab.yodaqa.save_answer1fvs="$xmidir"1 \
	$system_property \
	$args1

if [ -z "$basecommit" ]; then
	# In case of "$basecommit", re-gzip could be racy with another
	# train-and-eval running in parallel.
	gzip -f "$base_xmidir"/*.xmi &
	if [ "$retrain" = 1 ]; then
		gzip -f "$base_atrainfile0" &
		gzip -f "$csvdir"/*.csv &
	fi
fi

train_and_sync "1" "$atrainfile1" "$modelfile1"

# Re-score with new model
time ./gradlew tsvgs \
	-PexecArgs="$basedir/data/eval/${dataset}.tsv $outfile1" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug \
	-Dcz.brmlab.yodaqa.load_answer1fvs="$xmidir"1 \
	-Dcz.brmlab.yodaqa.save_answer1fvs="$xmidir"1 \
	$system_property \
	$args1


time ./gradlew tsvgs \
	-PexecArgs="$basedir/data/eval/${dataset}.tsv $outfile2" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug \
	-Dcz.brmlab.yodaqa.load_answer1fvs="$xmidir"1 \
	-Dcz.brmlab.yodaqa.save_answer2fvs="$xmidir"2 \
	$system_property \
	$args2

gzip -f "$base_xmidir"1/*.xmi &
if [ "$retrain" = 1 ]; then
	gzip -f "$atrainfile1" &
	gzip -f "$csvdir1"/*.csv &
fi

train_and_sync "2" "$atrainfile2" "$modelfile2"

# Re-score with new model
time ./gradlew tsvgs \
	-PexecArgs="$basedir/data/eval/${dataset}.tsv $outfile2" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug \
	-Dcz.brmlab.yodaqa.load_answer2fvs="$xmidir"2 \
	-Dcz.brmlab.yodaqa.save_answer2fvs="$xmidir"2 \
	$system_property \
	$args2

gzip -f "$base_xmidir"2/*.xmi &
if [ "$retrain" = 1 ]; then
	gzip -f "$atrainfile2" &
	gzip -f "$csvdir2"/*.csv &
fi

echo "$outfile2"

} 2>&1 | tee "$basedir/logs/${dataset}-${cid}.log"
gzip -f "$basedir/logs/${dataset}-${cid}.log"
