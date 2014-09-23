#!/bin/bash

showstats() {
	tsvout="$1"

	# XXX: if no answer generated, the answer line may be
	# completely missing
	total="$(wc -l <"$tsvout")"
	if [ "$total" -eq 0 ]; then
		echo 'no answers generated'
		return
	fi

	perfect="$(cat "$tsvout" | cut -f4 | grep '^1\.0' | wc -l)"
	any="$(cat "$tsvout" | cut -f4 | grep -v '^0\.0$' | wc -l)"

	perfectp="$(echo "100*$perfect/$total" | bc -l)"
	anyp="$(echo "100*$any/$total" | bc -l)"

	avgscore="$(echo "($(cat "$tsvout" | cut -f4 | tr '\n' '+')0)/$total" | bc -l)"
	avgtime="$(echo "($(cat "$tsvout" | cut -f2 | tr '\n' '+')0)/$total" | bc -l)"

	printf '%d/%d/%d %.1f%%/%.1f%% avgscore %.3f avgtime %.3f\n' \
		"$perfect" "$any" "$total" "$perfectp" "$anyp" "$avgscore" "$avgtime"
}

if [ "$#" -gt 0 ]; then
	for tsvout; do
		printf '%s ' "$tsvout"
		showstats "$tsvout"
	done

else
	# List all commits that have recorded evaluation
	evaldir=$(dirname "$0")/tsv
	commits=$(echo "$evaldir"/*.tsv | sed 's/[^ ]*-\([^.]*\).tsv/\1/g')
	git log --no-walk --pretty='tformat:%h %ad %s' --date=short $commits |
		while read commit date subject; do
			for file in "$evaldir"/*-"$commit".tsv; do
				name="${file##*/}"; name="$(echo "$name" | cut -d- -f2)"
				printf '% 5s %s %s %.20s... ' "${name:0:5}" "$commit" "$date" "$subject"
				showstats "$file"
			done
		done
fi | less -F
