STS for Sentence Pair Scoring
=============================

We use the STS framework https://github.com/brmson/dataset-sts to apply
deep neural network models on the problem of sentence pair scoring, which
appears in various places of the YodaQA pipeline; in particular, when
scoring potential answer-bearing passages and when scoring answre-leading
property paths.

By default, our STS API endpoint is used (sts.ailao.eu:5050).  You can modify
this to yours by editing src/main/java/cz/brmlab/yodaqa/provider/STSScoring.java
appropriately.  You need to set up dataset-sts and its libraries, train a model
or download yours.

Passage Scoring
---------------

First major usage of STS is for scoring passages, instead of a very simplistic
keyword based PassScoreSimple.  Download

	http://pasky.or.cz/dev/brmson/weights-anssel-termfreq-3368350fbcab42e4-bestval.h5

and run

	tools/scoring-api.py termfreq anssel data/anssel/yodaqa/large2470-training.csv weights-anssel-termfreq-3368350fbcab42e4-bestval.h5

This is a BM25 model trained on large2470-train.
