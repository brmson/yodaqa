#!/bin/bash
#
# Generate a data/eval/rankgraph.eps file showing portion of questions
# with correct answer at a given or better rank, depending on that rank.

data/eval/tsvout-ranks.sh "$1" |
	cut -f 1,4 | {
	lasti=0
	while read x y; do
		echo $x $y
		lasti=$x
	done
	for i in `seq $(($lasti + 1)) $(cat "$1" | wc -l)`; do
		echo $i NaN
	done; } >data/eval/ranks.dat
gle -cairo data/eval/ranks.gle
