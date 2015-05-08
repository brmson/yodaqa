#!/usr/bin/perl
# From the XML dataset, filter out just questions which are marked
# as "out of scope".

use warnings;
use strict;

use List::Util qw(shuffle);

my ($inf, $outf) = @ARGV;

my %questions;

open my $f, $inf or die "$inf: $!";
my $qi = 0;
while (<$f>) {
	if (/^<question.* id="(.*?)"/) {
		$qi = $1;
	}
	next if $qi == 0;
	$questions{$qi} .= $_;
	$qi = 0 if /^<\/question>/;
}

sub write_split {
	my ($fname, @qids) = @_;
	print(join(' ', @qids));
	open my $f, '>', $fname or die "$f: $!";
	print $f '<?xml version="1.0"?>'."\n";
	print $f '<dataset id="qald5_oss">'."\n";
	for my $qid (@qids) {
		next unless $questions{$qid} =~ /OUT OF SCOPE/;
		print $f "\n".$questions{$qid};
	}
	print $f "\n".'</dataset>'."\n";
}
write_split($outf, sort keys %questions);
