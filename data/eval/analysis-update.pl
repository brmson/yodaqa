#!/usr/bin/perl
# Make a copy of the recall analysis report (on stdin) to reflect new results.
# Recalled entries will be removed, newly missing entries appended at the end
# of the file.
# Note that you still need to update numbers in the solution index. Use vimdiff
# to review changes in analysis.

use warnings;
use strict;
use v5.10;

if (@ARGV != 1) {
	say STDERR "Usage: $0 RESULTFILE.TSV <ANALYSIS_OLD >ANALYSIS_NEW";
	exit(1);
}

sub load_resultfile {
	my ($resultfile) = @_;
	my %results;
	open my $rf, $resultfile or die "$!";
	while (<$rf>) {
		chomp;
		my @result = split /\t/;
		my ($id, $rank) = @result[0,4];
		next unless $rank == -1; # skip recalled questions
		$results{$id} = $_;
	}
	close $rf;
	return %results;
}

sub main {
	my ($resultfile) = @_;
	my %results = load_resultfile($resultfile);

	my $state = 'scan';
	while (<STDIN>) {
		chomp;
reprocess:
		if ($state eq 'scan') {
			if (/^(\d+)\t/) {
				my $id = $1;
				if (exists $results{$id}) {
					# Copy over, but print updated version
					say $results{$id};
					delete $results{$id};
				} else {
					$state = 'hold';
				}
			} else {
				say $_;
			}
		} else { # $state eq 'hold'
			if (/^\s/) {
				# hold
			} else {
				# end of hold, resume scan+copy
				$state = 'scan';
				goto reprocess;
			}
		}
	}

	# Now spit out remaining results
	for my $id (sort { $a <=> $b } keys %results) {
		say $results{$id};
	}
}

main(@ARGV);
