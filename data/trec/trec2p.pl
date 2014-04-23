#!/usr/bin/perl
# Convert TREC1999-2003 patterns sets from their custom format
# to answer-per-line TSV; multiple patterns are |-ed.

# This is basically a prettified oneliner so it's ugly, sorry.

use v5.10;

while (<>) {
	chomp;
	($a, $b) = ($_ =~ /^(\d+) (.*)/);
	if (defined $la and $a == $la) {
		$lb .= '|' . $b;
	} else {
		say "$la\t$lb";
		$la = $a;
		$lb = $b;
	}
}
say "$la\t$lb";
