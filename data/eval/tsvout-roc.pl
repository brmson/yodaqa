#!/usr/bin/perl
#
# Analyze precision/recall tradeoffs when setting a confidence threshold
# for issuing an answer.  In this version, only the top answer is considered.
#
# Shows precision/recall-tradeoff for each score threshold, and prints data
# for tradeoff and ROC curves.  Generate these using:
#
# 	data/eval/tsvout-roc.pl data/eval/tsv/moviesD-test-ovt-9d2039a.tsv; gle data/eval/prt.gle data/eval/roc.gle; evince data/eval/prt.eps data/eval/roc.eps

use warnings;
use strict;
use v5.10;
use autodie;

my @ans;
my $tot = 0;
while (<>) {
	$tot++;
	@_ = split /\t/;
	my $correct = $_[4] == 0 ? 1 : 0;
	my $score = (split(/:/, $_[13]))[-1];
	next unless defined $score;
	#say $_[4], " ", $_[13], " ", $correct, " ", $score;
	push @ans, [$score, $correct];
}

my @sans = sort { $b->[0] <=> $a->[0] } @ans;

open my $gleprt, '>', "data/eval/prt.dat";
open my $gleroc, '>', "data/eval/roc.dat";

my $threshold = 1.0;
my $n_correct = 0;
for my $i (0..$#sans) {
	my ($score, $correct) = @{$sans[$i]};
	$n_correct += $correct;

	my $n = $i+1;
	printf "[Threshold %.4f] N=%d, TP=%.3f, FP=%.3f, prec=%.3f, recall=%.3f\n",
		$threshold, $n, $n_correct/$tot, ($n-$n_correct)/$tot, $n_correct/$n, $n_correct/$tot;
	printf $gleprt "%f %f\n", $n_correct/$tot, $n_correct/$n;
	printf $gleroc "%f %f\n", ($n-$n_correct)/$tot, $n_correct/$tot;

	if ($score < $threshold) {
		$threshold = $score;
	}
}

printf "[Threshold %.4f] N=%d, prec=%.3f, recall=%.3f\n",
	$threshold, scalar @sans, $n_correct/@sans, $n_correct/$tot;
