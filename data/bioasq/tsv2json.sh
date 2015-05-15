#!/bin/bash
# cat data/eval/tsv/bioasq-submission-ovt-u08e0a10.tsv | ...
# XXX: before submission, you will need to delete the final ,{} apparently
echo '{"questions":['
sed 's/:[0-9.]*//g' |
	while IFS=$'\t' read id x q x x x x x x x x x a0 a1 a2 a3 a4 x; do
		type=$(perl -MJSON -e 'my $j = decode_json `cat data/bioasq/BioASQ-task3bPhaseB-testset5`; for $q (@{$j->{questions}}) { if ($q->{id} eq $ARGV[0]) { print $q->{type}."\n"; } }'  $id)
		if [ $type = factoid -o $type = list ]; then
			ea="[[\"$a0\"], [\"$a1\"], [\"$a2\"], [\"$a3\"], [\"$a4\"]]"
		elif [ $type = yesno ]; then
			ea="\"yes\""
		else
			ea="[]"
		fi
		echo '{"id":"'$id'", "exact_answer":'$ea', "ideal_answer":""},'
	done | sed 's/\(, \)*\["[wW]e"\]//g; s/\[, /\[/g'
echo '{}]}'
