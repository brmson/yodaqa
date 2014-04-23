#!/usr/bin/perl
# Convert TREC1999-2003 questions sets from their custom format
# to question-per-line TSV.

# This is basically a prettified oneliner so it's ugly, sorry.

use v5.10;

while (<>) {
	if (m#^<num> Number: (\d+)#) {
		$a[0] = $1;

	} elsif (m#^<type> Type: (\w+)#) {
		$a[1] = $1;

	} elsif (m#^<desc> #) {
		$b = "d";

	} elsif (m#^</top>#) {
		$a[1] ||= "factoid";
		say join("\t", @a);
		@a=(); $b="";

	} elsif ($b eq "d") {
		chomp;
		$a[2] .= " " if $a[2];
		$a[2] .= $_;
	}
}
