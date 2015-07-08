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

	perfect="$(cat "$tsvout" | cut -f5 | grep '^0$' | wc -l)"
	any="$(cat "$tsvout" | cut -f5 | grep -v '^-1$' | wc -l)"

	perfectp="$(echo "100*$perfect/$total" | bc -l)"
	anyp="$(echo "100*$any/$total" | bc -l)"

	mrr="$(echo "($(cat "$tsvout" | cut -f5 | sed 's/-1/10000/; s/.*/1\/(1+&)/' | tr '\n' '+')0)/$total" | bc -l)"
	avgtime="$(echo "($(cat "$tsvout" | cut -f2 | tr '\n' '+')0)/$total" | bc -l)"

	printf '%d/%d/%d %.1f%%/%.1f%% mrr %.3f avgtime %.3f\n' \
		"$perfect" "$any" "$total" "$perfectp" "$anyp" "$mrr" "$avgtime"
}

if [ "$#" -gt 1 ]; then
	for tsvout; do
		printf '%s ' "$tsvout"
		showstats "$tsvout"
	done

else
	# List all commits that have recorded evaluation
	evaldir=$(dirname "$0")/tsv
	commits=$(echo "$evaldir"/*$1*.tsv | sed 's/[^ ]*-\([^.]*\).tsv/\1/g; s/[uvw]//g')
	git log --no-walk --pretty='tformat:%h %ad %s' --date=short $commits |
		while read commit date subject; do
			for file in "$evaldir"/*"$commit".tsv; do
				case $file in *"$1"*) ;; *) continue;; esac
				name="${file##*/}";
				commit="${file##*/}"; commit="$(echo "$commit" | cut -d- -f4)"; commit="${commit%.*}"
				printf '% 12s % 8s %s %.20s... ' "${name:0:12}" "$commit" "$date" "$subject"
				showstats "$file"
			done
		done
fi | less -F
