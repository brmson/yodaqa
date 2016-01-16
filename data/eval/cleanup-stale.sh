#!/bin/bash
# Clean-up stale experimental data like logs, CAS dumps etc.
# These may well take up to a gigabyte or two per run, quickly piling up.
# This will remove all but the last 20 such files.

set -e

for d in logs/ data/ml/tsv/ data/eval/answer*-csv/ data/eval/answer-xmi/; do
	n_files=$(($(ls -rtd $d/* | wc -l) - 20))
	if [ $n_files -lt 1 ]; then
		echo "$d: Too few files."
		continue
	fi
	ls -rtd $d/* | head -n $n_files | xargs rm -rv
done
