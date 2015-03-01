Performance Evaluation
======================

This directory is dedicated to data and scripts related to the records of
performance evaluation runs at various commits - to measure the data and
show some simple statistics.

By default, the evaluation is run with the setup exactly at that commit,
including the data source (enwiki dump of the specified date etc.).

This directory also used to contain the measurements themselves, but there
is too many of them at this point.  They are archived at

	http://pasky.or.cz/dev/brmson/yodaqa-eval/

and they should be stored in the data/eval/tsv/ directory.

Datasets
--------

Our base dataset of questions consists of 867 questions that were adapted
by us from the TREC QA track dataset and a #brmson IRC dataset; the raw
datasets and detailed notes can be found in the data/trec/ directory.

For evaluation purposes, we shuffle the questions randomly

	cat ../trec/trecnew-curated.tsv ../trec/irc-curated.tsv | shuf >curated-full.tsv

and then use the first 430 questions for the training dataset curated-train.tsv
and the following 430 questions for the test dataset curated-test.tsv.
The extra 7 questions are reserved for further use.

These two datasets have the following designation:

  * Training dataset data/eval/curated-train.tsv:  We use this dataset for
    primary development, detailed performance analysis and training (and
    if possible also testing) machine learning algorithms.

  * Testing dataset data/eval/curated-test.tsv:  We use this dataset for
    benchmarking YodaQA performance.  We attempt to treat this dataset as
    "blind" and do not analyze or optimize performance for individual
    questions in this dataset.

Ideally, humans should be doing all stages of evaluation instead of just
using regex matches, as time by time an unconcieved legitimate answer
pops up and on the other hand, sometimes the regex is unintentionally
over-permissive.

N.B. up to early September 2014, we were using a "trecnew-single200"
dataset - we used it for both the "training" and "testing" purposes
and it was uncurated, i.e. with many unsatisfactory answer patterns
and suspect questions.

Tools
-----

To re-train models and benchmark training + testing set performances
at a given commit (uncommitted changes are ignored!), run

	data/eval/train-and-eval.sh

from the project root.  It will create a file in data/eval/ with
the answers to the training set questions.  To display simple stats
on these files, run

	data/eval/tsvout-stats.sh data/eval/tsv/curated-test-*.tsv

or, to show all recorded evaluations chronologically, simply

	data/eval/tsvout-stats.sh
	data/eval/tsvout-stats.sh train
	data/eval/tsvout-stats.sh test

To compare two performance measurements question-by-question,
try running something like (in either style):

	data/eval/tsvout-compare.sh data/eval/tsv/curated-train-out-0b086cf.tsv data/eval/tsv/curated-train-out-1a80ccd.tsv
	data/eval/tsvout-compare.sh 0b086cf 1a80ccd

To show statistics based on amount of questions sporting the
correct answer at a given rank, run:

	data/eval/tsvout-ranks.sh data/eval/tsv/curated-train-ovt-3b46430.tsv
	data/eval/tsvout-rankgraph.sh data/eval/tsv/curated-train-ovt-3b46430.tsv

Legacy Benchmarking
-------------------

(If you want to avoid re-training models and want to have more control
over the process, there is a more fine-grained approach available.
To measure training set performance at a given commit, run

	data/eval/curated-measure.sh train

and to benchmark performance on test set, run

	data/eval/curated-measure.sh test

and then investigate the results as above.)

When debugging the AnswerGSHook mechanisms etc., it may be also useful
to do a custom mini tsvgs run, like:

	./gradlew tsvgs -Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug \
		-PexecArgs="data/eval/curated-toptwo.tsv toptwo.tsv" \
		-Dcz.brmlab.yodaqa.train_answer=training-answer.tsv \
		-Dcz.brmlab.yodaqa.train_answer1=training-answer1.tsv \
		-Dcz.brmlab.yodaqa.train_answer2=training-answer2.tsv \
		-Dcz.brmlab.yodaqa.save_answer2fvs=answers2  2>&1 | \
		tee /tmp/yodaqa.log

(make `data/eval/curated-toptwo.tsv` from some other gold standard
TSV file).

Analysis
--------

### Recall Analysis

We are analyzing questions that failed recall in the analysis/
subdirectory.  Systematic analysis is important especially for
headroom (potential improvement) estimation of features that
we can focus on next.  When updating to a new revision, you can
reuse previous analysis results, e.g.:

	cd data/eval
	analysis-update.pl tsv/curated-train-ovt-7b2a3f9.tsv \
		<analysis/recall-curated-train-ovt-184ebbb.txt \
		>analysis/recall-curated-train-ovt-7b2a3f9.txt

See the top of that scripts for some extra notes.

### Answer Feature Analysis

#### Feature Vector

Each candidate answer for each evaluated question is assigned a feature
vector (which is already growing quite long) and values of vector elements
determine the score of the answer.  For each question, feature vectors
of answers are stored in data/eval/answer-csv/COMMIT/QID.csv.

If you open this in libreoffice, position cursor on cell B2 and use the
Window -> Freeze tool for easier inspection.  To extract some specific
columns, csvtool can be convenient:

	csvtool namedcol @noTyCor,@LATANone,q,answer

Many of the brmson bugfixes and improvements are about adding a feature
or fixing its generation - then it makes sense to compare the feature
vectors of answers between two commits:

	data/ml/answer-comparefv.py data/eval/answer-csv/COMMIT1 data/eval/answer-csv/COMMIT2 | less -S

#### Weight Vector

The answer score is computed by multiplying the feature vector with
the weight vector; the weight vector is stored in

	src/main/resources/cz/brmlab/yodaqa/analysis/ansscore/AnswerScoreLogistic.model

and determined from the training set using data/ml/ toolkit, the new
weights are then stored in:

	data/ml/models/logistic-COMMIT.model

If you tweak some features, it can be roughly estimated how good
a predictor they are by comparing how the weights change.

TODO: Build a tool that compares two models, vimdiff is clunky for this.
