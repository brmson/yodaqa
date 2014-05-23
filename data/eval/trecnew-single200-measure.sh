#!/bin/sh

outfile="data/eval/tsv/trecnew-single200-ovt-$(git rev-parse --short HEAD).tsv"
mkdir -p data/eval/tsv

head -n 200 data/trec/trecnew-single.tsv >data/trec/trecnew-single200.tsv
mvn verify exec:java -Ptrecgs \
	-Dexec.args="data/trec/trecnew-single200.tsv $outfile"
echo $outfile
