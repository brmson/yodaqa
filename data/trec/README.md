TREC QA Data
============

The now-discontinued QA track of the NIST TREC conference provides
the de-facto standard benchmark data for QA systems:

	http://trec.nist.gov/data/qa.html

This directory contains some download and format conversion scripts
(run ``trec-setup.sh`` to download TREC datasets for several years
and produce some easy-to-process TSV files) and also the reference
(mostly) TREC-based datasets:

  * ``trecnew-all.tsv`` contains ID, type, question and answer PCRE
    for questions coming from TREC 11 and 12 (years 2002, 2003),
    which appear to be the most mature and corpus-agnostic sets.

  * ``trecnew-all-comments.txt`` contains curation notes for the
    questions - basically analysis of questions that are deemed
    as unapplicable for being too source-specific, with inaccurate
    answer pattern or that is unanswerable, plus notes about changes
    made to the dataset

  * ``trecnew-curated.tsv`` is a version of the ``trecnew-all.tsv``
    with all the changes described in comments.txt applied, i.e.
    updated answer patterns, some questions reworder and quite a few
    removed altogether

  * ``irc-curated.tsv`` is not really a TREC but a set of questions
    that have been asked in early 2014 on the #brmson freenode IRC
    channel (where a BlanQA implementation was running for a time),
    with duplicates and non-questions removed and the rest curated
    similarly to the trecnew dataset; they generally go in slightly
    different directions, sometimes are intentionally trivial and
    contain no mention of sports

Note that historically (up to early Sep 2014), we used slightly
different dataset arrangement.  Refer to the git history if you need.

Origin and Licencing
--------------------

These sets were produced by NIST, a US Government institution, which
makes them public domain.  Ellen M. Voorhees of NIST kindly confirmed:

	> > The questions and answer patterns are freely download-able
	> > from the TREC web site. ...
	> ... Could you please clarify what of what copyright status
	> they are and what their licence is?
	They are in the public domain, though, of course, we would
	appreciate attribution.

The answer PCRE patterns have been originally contributed to the NIST
TREC QA track as a courtesy of Ken Litkowski of CL Research.
