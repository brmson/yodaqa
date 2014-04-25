Performance Evaluation
======================

This directory contains archived data of performance evaluation runs
at various commits, plus related scripts to measure the data and show
some simple statistics.

By default, the evaluation is run with the setup exactly at that commit,
including the data source (enwiki dump of the specified date etc.).

Tools
-----

To measure performance at a given commit, run

	data/eval/trecnew-single200-measure.sh

from the project root.  It will create a file in data/eval/ with
the answers to a set of 200 trecnew-single questions.  To display
simple stats on these files, run

	data/eval/tsvout-stats.sh data/eval/trecnew-single200-*.tsv
