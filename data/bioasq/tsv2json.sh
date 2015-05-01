#!/bin/bash
# cat data/eval/tsv/bioasq-submission-ovt-u08e0a10.tsv | ...
echo '{"questions":['
sed 's/:[0-9.]*//g' |
	while IFS=$'\t' read id x q x x x x x x x x x a0 a1 a2 a3 a4 x; do
		type=$(perl -MJSON -e 'my $j = decode_json `cat data/bioasq/BioASQ-task3bPhaseB-testset4`; for $q (@{$j->{questions}}) { if ($q->{id} eq $ARGV[0]) { print $q->{type}."\n"; } }'  $id)
		[ $type != summary ] || continue
		if [ $type = factoid ]; then
			ea="[\"$a0\", \"$a1\", \"$a2\", \"$a3\", \"$a4\"]"
		else
			ea="[[\"$a0\"], [\"$a1\"], [\"$a2\"], [\"$a3\"], [\"$a4\"]]"
		fi
		echo '{"id":"'$id'", "exact_answer":'$ea'},'
	done
echo '{}]}'
