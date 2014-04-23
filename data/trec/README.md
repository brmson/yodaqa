TREC QA Data
============

The now-discontinued QA track of the NIST TREC conference provides
the de-facto standard benchmark data for QA systems:

	http://trec.nist.gov/data/qa.html

This directory contains some download and format conversion scripts
(run ``trec-setup.sh`` to download TREC datasets for several years
and produce some easy-to-process TSV files) and also two reference
TREC-based datasets:

  * ``trecnew-all.tsv`` contains ID, type, question and answer PCRE
    for questions coming from TREC 11 and 12 (years 2002, 2003),
    which appear to be the most mature and corpus-agnostic sets.

  * ``trecnew-single.tsv`` is a subset of the above that contains
    only questions that have just a single matching answer.

Note that the datasets might be slightly customized or modified,
they are more than 10 years old after all; consult the git logs
to check this.

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
