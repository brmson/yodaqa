#!/usr/bin/perl

use warnings;
use strict;

use List::Util qw(shuffle);

my ($inf, $split0n, $split0f, $split1n, $split1f) = @ARGV;

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
	open my $f, '>', $fname or die "$f: $!";
	print $f '<?xml version="1.0"?>'."\n";
	print $f '<dataset id="qald5_split">'."\n";
	for my $qid (@qids) {
		next if $questions{$qid} =~ /OUT OF SCOPE/;
		print $f "\n".$questions{$qid};
	}
	print $f "\n".'</dataset>'."\n";
}

my @shuffled_id = shuffle(keys %questions);
my @split0 = @shuffled_id[0..$split0n-1];
write_split($split0f, @split0);
my @split1 = @shuffled_id[$split0n..$split0n+$split1n-1];
write_split($split1f, @split1);
