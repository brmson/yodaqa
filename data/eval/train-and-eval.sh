#!/bin/bash
#
# Usage: data/eval/train-and-eval.sh [COMMIT]
#
# Perform full model training and performance evaluation of the given
# commit (may be also a branch name, or nothing to eval the HEAD).
# The training and test set is processed in parallel, however the
# final answer evaluation is delayed until the model is retrained.
#
# This produces answer TSV files for training and test set
# (final, and pre-training ones with 'u' prefix before commit),
# xmi answer feature vectors file, a model parameter file and a log
# for both the train and test runs.
#
# If the evaluation has been successful, you should commit an updated
# model; that's one of the last lines printed by the list.
#
# Since this all involves multiple executions and we often want to
# evaluate multiple versions at once, we create a temporary clone of
# the current repo and run things there.  N.B. uncommitted changes
# are *not* tested!  The actual execution happens in the script
# `data/eval/_multistage_traineval.sh`.

set -e

cid=$(git rev-parse --short "${1:-HEAD}")
baserepo=$(pwd)

clonedir="../yodaqa-te-$cid"
if [ -e "$clonedir" ]; then
	ls -ld "$clonedir"
	echo "$clonedir: Directory where we would like to clone exists, try again after" >&2
	echo "rm -rf \"$clonedir\"" >&2
	exit 1
fi

git clone "$baserepo" "$clonedir"
pushd "$clonedir"
git checkout "$cid"
ln -vs "$baserepo"/data/wordnet/* data/wordnet/

echo "Checked out in $clonedir"
sleep 2

# Pre-build so we don't do that twice
time mvn verify

echo "Starting evaluation in $clonedir"
sleep 2

screen -m sh -c '
	screen "'"$baserepo"'/data/eval/_multistage_traineval.sh" "'"$baserepo"'" "train";
	sleep 10;
	screen "'"$baserepo"'/data/eval/_multistage_traineval.sh" "'"$baserepo"'" "test"
'

popd

data/eval/tsvout-stats.sh "$cid"
modelfile="data/ml/models/logistic-${cid}.model"
echo
echo "Now, you may want to do and commit:"
echo "cp $modelfile src/main/resources/cz/brmlab/yodaqa/analysis/answer/AnswerScoreLogistic.model"
echo
echo "Run finished. Press Enter to rm -rf \"$clonedir\"; Ctrl-C to preserve it for whatever reason (data and logs are not kept there)."
read
rm -rf "$clonedir"
