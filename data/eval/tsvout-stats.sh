#!/bin/sh

for tsvout; do
	# XXX: if no answer generated, the answer line may be
	# completely missing
	#total="$(wc -l <"$tsvout")"
	total=200

	perfect="$(cat "$tsvout" | cut -f3 | grep '^1\.0' | wc -l)"
	any="$(cat "$tsvout" | cut -f3 | grep -v '^0\.0$' | wc -l)"

	perfectp="$(echo "100*$perfect/$total" | bc -l)"
	anyp="$(echo "100*$any/$total" | bc -l)"

	avgscore="$(echo "($(cat "$tsvout" | cut -f3 | tr '\n' '+')0)/$total" | bc -l)"

	printf '%s %d/%d/%d %.1f%%/%.1f%% avgscore %.3f\n' "$tsvout" "$perfect" "$any" "$total" "$perfectp" "$anyp" "$avgscore"
done
