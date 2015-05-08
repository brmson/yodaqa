#!/usr/bin/perl
#
# Example: data/qald/tsv2xml.pl qald-5_train <data/eval/tsv/qald5-test-ovt-u589ed8c.tsv
# Example: cat data/eval/tsv/qald5-train-ovt-ueb4b2ac.tsv data/eval/tsv/qald5-test-ovt-ueb4b2ac.tsv | sort -n | data/qald/tsv2xml.pl qald-5_train

use v5.10;
use warnings;
use strict;

my ($dsid) = @ARGV;

print <<EOT;
<?xml version="1.0"?>
<dataset id="$dsid">

EOT

while (<STDIN>) {
	chomp;
	# 3       1670.436        Who is the youngest Pulitzer Prize winner?      0.0     -1      22      _       .       0       0       0       0       http://dbpedia.org/resource/Columbia_University:0.04280051778952415     http://dbpedia.org/resource/United_States:0.008841171116406175  http://dbpedia.org/resource/Audrey_Wurdemann:0.005731326386699326       http://dbpedia.org/resource/Joan_Allen:0.005242803233491832     http://dbpedia.org/resource/Chude_Jideonwo:0.0026083984576321982        http://dbpedia.org/resource/A._M._Rosenthal:0.002584942202843058        http://dbpedia.org/resource/Stass_Shpanin:0.0025782261197086847 http://dbpedia.org/resource/Gilles_Chiasson:0.002577425050341983        http://dbpedia.org/resource/Sadi_Ranson:0.002574137204367147    http://dbpedia.org/resource/After_This:0.0022287682961872712    http://dbpedia.org/resource/The_Awakening_Land_trilogy:0.0022268654221864084    http://dbpedia.org/resource/Jon_McGregor:0.0014137651908433806  http://dbpedia.org/resource/Jerry_Mitchell_(investigative_reporter):0.0014136355135592712       http://dbpedia.org/resource/Will_Sullivan:0.0014128653923412833 http://dbpedia.org/resource/William_Bernhardt:0.00141128486125676
	my ($id, $t, $q, $s, $r, $n, $p, $c, $e0, $e1, $e2, $e3, @a) = split(/\t/);
	my (@atext) = map { s/^(.*):.*?$/$1/; s/&/&amp;/g; $_; } @a;
	if ($q !~ /^(Give|List|Show)/ and @atext > 0) {
		@atext = ($atext[0]);  # just the first answer
	}
	my $hybrid = $id > ($dsid =~ /test/ ? 50 : 300) ? 'true' : 'false';
	say('<question id="'.$id.'" hybrid="'.$hybrid.'">');
	say('<string lang="en"><![CDATA['.$q.']]></string>');
	say('<answers>');
	for my $a (@atext) {
		say('<answer>'.$a.'</answer>');
	}
	say('</answers>');
	say('</question>'."\n");
}

say('</dataset>');
