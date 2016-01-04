Governing Licences
==================

This project and all contributions are per se dual-licenced under the:

  * ASLv2 licence (LICENCE-ASL2.txt)

  * GPLv3 (LICENCE-GPL3.txt) or any later version of the GPL licence

You can choose, at your option, to follow the terms of either or produce
derivative works licenced under the terms of either, however you are
encourged to dual-licence your derivative works in the same way so that
code can be merged back to the original project unhampered.


Rationale
---------

We prefer the ASLv2 licence, which is the common licence in the Java NLP
ecosystem and we would like to encourage wide usage of our framework;
we would appreciate it very much if all users would make their versions
of the framework open source (and we believe QA/NLP researchers using
the system *should* do it), but we do not want to scare away company
users by making it a legal requirement, with the hope that they will
still contribute back as much as they can.

The primary reason for dual-licencing is the fact that some components
are licenced under the GPLv2+ licence.  Therefore, a binary distribution
of this project that is built without modifications includes calls to
(third-party, source not included in this project) GPLv2 components and
may require to follow the GPLv3 licence!

However, in all cases the GPLv2 components are not essential, are
isolated through a generic (non-GPL'd) API, and can be removed without
rendering the system useless (just at the cost of some performance).

These components are GPLv2+:

  * Stanford CoreNLP - used through DKpro generic API mainly for
    producing dependency parse trees of sentences, other alternatives
    with worse performance exist

  * ClearTK CRFSuiteWrapper - used through ClearTK generic API, used
    only for training a CRF model (only when YodaQA is invoked as shown
    in data/ml/biocrf/README.md in particular; *not* used for CRF-based
    biotagging during regular YodaQA usage)


Acknowledgements
----------------

The software is copyrighted by its respective authors as denoted in the
publicly recorded Git history.

Work on YodaQA has been in part kindly sponsored by the Medialab
foundation (http://medialab.cz/), a Czech Technical University incubator.
The rest of contributions by Petr Baudiš is licenced as open source via
Ailao (http://ailao.eu/).

Ailao also provides commercial consulting, customization, deployment and
support services.

YodaQA uses many libraries to get things done.
It is built on top of the Apache UIMA and DKpro UIMA bindings, used
for most of the NLP logic (including the NLP type system);
transitively, work like the StanfordParser and Princeton's Wordnet is crucial
for us.  For machine learning, we use chiefly scikit-learn, but also a little
of crfsuite (wrapped by ClearTK, jcrfsuite).
