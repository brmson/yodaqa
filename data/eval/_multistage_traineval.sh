#!/bin/bash
#
# Usage: _multistage-traineval.sh BASEDIR {train,test} [BASECOMMIT]
#
# In training mode, run the pipeline for all questions up to the
# answer scoring, stop there, perform training, update the model
# and re-run just the answer scoring to get the final results.
#
# Testing mode (evaluation on test set) is designed to run in
# parallel with the training mode and works in lock-step, stopping
# before scoring to wait and use the new model.
#
# If BASECOMMIT is specified, the pipeline phase 0 is not run
# but instead data files are symlinked from BASECOMMIT evals.
#
# All data and logs are stored relative to BASEDIR.  The project
# is assumed to be built already (using `gradle check`).  This is
# meant to be used via the data/eval/train-and-eval.sh script.

## Setup and initialize

cid=$(git rev-parse --short HEAD)

basedir="$1"
type="$2"
basecommit="$3"
args0=
argsF=

outfile0="$basedir/data/eval/tsv/curated-${type}-ovt-u${cid}.tsv"
outfile1="$basedir/data/eval/tsv/curated-${type}-ovt-v${cid}.tsv"
outfile2="$basedir/data/eval/tsv/curated-${type}-ovt-w${cid}.tsv"
outfileF="$basedir/data/eval/tsv/curated-${type}-ovt-${cid}.tsv"
atrainfile0="$basedir/data/ml/tsv/training-answer-${cid}.tsv"
atrainfile1="$basedir/data/ml/tsv/training-answer1-${cid}.tsv"
atrainfile2="$basedir/data/ml/tsv/training-answer2-${cid}.tsv"
modelfile0="$basedir/data/ml/models/logistic-${cid}.model"
modelfile1="$basedir/data/ml/models/logistic1-${cid}.model"
modelfile2="$basedir/data/ml/models/logistic2-${cid}.model"
xmidir="$basedir/data/eval/answer-xmi/${cid}-$type"
barrierfile=_multistage-barrier
mkdir -p $basedir/data/eval/tsv
mkdir -p $basedir/data/eval/answer-csv
mkdir -p $basedir/data/eval/answer1-csv
mkdir -p $basedir/data/eval/answer2-csv
mkdir -p $basedir/data/ml/models
mkdir -p "$xmidir" "$xmidir"1 "$xmidir"2

case $type in
	test) ;;
	train)
		args0="-Dcz.brmlab.yodaqa.train_passextract=$basedir/data/ml/tsv/training-passextract-${cid}.tsv
		       -Dcz.brmlab.yodaqa.train_answer=$atrainfile0 -Dcz.brmlab.yodaqa.csv_answer=$basedir/data/eval/answer-csv/${cid}"
		args1="-Dcz.brmlab.yodaqa.train_answer1=$atrainfile1 -Dcz.brmlab.yodaqa.csv_answer1=$basedir/data/eval/answer1-csv/${cid}"
		args2="-Dcz.brmlab.yodaqa.train_answer2=$atrainfile2 -Dcz.brmlab.yodaqa.csv_answer2=$basedir/data/eval/answer2-csv/${cid}"
		;;
	*) echo "Usage: $0 BASEDIR {test,train}; but use the train-and-eval wrapper." >&2; exit 1;;
esac

if [ -e $barrierfile ]; then
	echo "$barrierfile: Already exists" >&2
	read x
	exit 1
fi

train_and_sync() {
	i=$1
	atrainfile=$2
	modelfile=$3

	## Train the model
	if [ "$type" = "train" ]; then
		echo "Training ${i}..."
		data/ml/answer-train.py <"$atrainfile" | tee "$modelfile"
		cp "$modelfile" src/main/resources/cz/brmlab/yodaqa/analysis/answer/AnswerScoreLogistic${i}.model
		echo "Rebuilding with new model..."
		gradle check
		touch "$barrierfile" # testing is go, too!

	else  # test
		# Just wait for the training to finish; XXX ugly this way
		echo "Waiting for $barrierfile, #${i}"
		while [ ! -e "$barrierfile" ]; do
			echo -n .
			sleep 2
		done
		echo " :)"
		rm "$barrierfile"
	fi
}

## Run the pipeline

{

if [ -z "$basecommit" ]; then
	## Gather answers once, also storing the answerfvs
	echo "First run..."
	time gradle tsvgs \
		-Dexec.args="$basedir/data/eval/curated-${type}.tsv $outfile0" \
		-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa.analysis=debug \
		-Dcz.brmlab.yodaqa.save_answerfvs="$xmidir" \
		$args0

else
	## Reuse data files from $basecommit
	base_outfile0="$basedir/data/eval/tsv/curated-${type}-ovt-u${basecommit}.tsv"
	base_xmidir="$basedir/data/eval/answer-xmi/${basecommit}-$type"
	ln -s "$base_outfile0" "$outfile0"
	rmdir "$xmidir"
	ln -s "$base_xmidir" "$xmidir"

	if [ "$type" = "train" ]; then
		base_atrainfile0="$basedir/data/ml/tsv/training-answer-${basecommit}.tsv"
		ln -s "$base_atrainfile0" "$atrainfile0"
		ln -s "$basedir/data/ml/tsv/training-passextract-${basecommit}.tsv" "$basedir/data/ml/tsv/training-passextract-${cid}.tsv"
		ln -s "$basedir/data/eval/answer-csv/${basecommit}" "$basedir/data/eval/answer-csv/${cid}"
	fi
fi

train_and_sync "" "$atrainfile0" "$modelfile0"

time gradle tsvgs \
	-Dexec.args="$basedir/data/eval/curated-${type}.tsv $outfile1" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa.analysis=debug \
	-Dcz.brmlab.yodaqa.load_answerfvs="$xmidir" \
	-Dcz.brmlab.yodaqa.save_answer1fvs="$xmidir"1 \
	$args1

train_and_sync "1" "$atrainfile1" "$modelfile1"

time gradle tsvgs \
	-Dexec.args="$basedir/data/eval/curated-${type}.tsv $outfile2" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa.analysis=debug \
	-Dcz.brmlab.yodaqa.load_answer1fvs="$xmidir"1 \
	-Dcz.brmlab.yodaqa.save_answer2fvs="$xmidir"2 \
	$args2

train_and_sync "2" "$atrainfile2" "$modelfile2"

time gradle tsvgs \
	-Dexec.args="$basedir/data/eval/curated-${type}.tsv $outfileF" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa.analysis=debug \
	-Dcz.brmlab.yodaqa.load_answer2fvs="$xmidir"2 \
	$argsF

echo "$outfileF"

} 2>&1 | tee "$basedir/logs/curated-${type}-${cid}.log"
