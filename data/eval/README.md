Performance Evaluation
======================

This directory is dedicated to scripts related to the records of performance
evaluation runs at various commits - to measure the data and show some simple
statistics.

By default, the evaluation is run with the setup exactly at that commit,
including the data source (enwiki dump of the specified date etc.).

This directory also used to contain the measurements themselves, but there
is too many of them at this point.  They are archived at

	http://pasky.or.cz/dev/brmson/yodaqa-eval/

and they should be stored in the data/eval/tsv/ directory.

Tools
-----

To measure performance at a given commit, run

	data/eval/trecnew-single200-measure.sh

from the project root.  It will create a file in data/eval/ with
the answers to a set of 200 trecnew-single questions.  To display
simple stats on these files, run

	data/eval/tsvout-stats.sh data/eval/tsv/trecnew-single200-*.tsv

or, to show all recorded evaluations chronologically, simply

	data/eval/tsvout-stats.sh

To compare two performance measurements question-by-question,
try running something like (in either style):

	data/eval/tsvout-compare.sh data/eval/tsv/trecnew-single200-out-0b086cf.tsv data/eval/tsv/trecnew-single200-out-1a80ccd.tsv
	data/eval/tsvout-compare.sh 0b086cf 1a80ccd

To show statistics based on amount of questions sporting the
correct answer at a given rank, run:

	data/eval/tsvout-ranks.sh data/eval/tsv/trecnew-single200-ovt-3b46430.tsv

Analysis
--------

We are analyzing questions that failed recall in the analysis/
subdirectory.  Systematic analysis is important especially for
headroom (potential improvement) estimation of features that
we can focus on next.  When updating to a new revision, you can
reuse previous analysis results, e.g.:

	cd data/eval
	analysis-update.pl tsv/trecnew-single200-ovt-7b2a3f9.tsv \
		<analysis/recall-trecnew-single200-ovt-184ebbb.txt \
		>analysis/recall-trecnew-single200-ovt-7b2a3f9.txt

See the top of that scripts for some extra notes.
