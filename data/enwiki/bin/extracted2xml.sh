#!/bin/sh
# Convert a set of WikiExtractor.py generated files to a single coherent
# XML file consumable by Solr.
#
# See README.md for usage instructions.

outfile=$1
if [ -z "$outfile" ]; then
	echo "Usage: $0 OUTFILE" >&2
	exit 1
fi

{
	echo '<wikitext>'
	find enwiki-text/ -name '*.bz2' | sort |
		while read f; do
			echo "$f" $(ls -lh "$outfile") >&2
			bzcat "$f"
		done
	echo '</wikitext>'
} >"$outfile"
