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

	http://pasky.or.cz/dev/brmson/weights-ubuntu-anssel80-rnn-3b7f4a294ad6f4c3-03-bestval.h5
	http://rover.ms.mff.cuni.cz/~pasky/ubuntu-dialog/v2-vocab.pickle.gz

and run

	tools/scoring-api.py rnn anssel v2-vocab.pickle weights-ubuntu-anssel80-rnn-3b7f4a294ad6f4c3-03-bestval.h5 "vocabt='ubuntu'" ptscorer=B.dot_ptscorer pdim=1

We have choosen 3b7f4a294ad6f4c3-03 as it was the model with highest val MRR
on val set in the 16-train run:

	data/anssel/yodaqa/large2470-training.csv Accuracy: raw 0.810384 (y=0 0.808999, y=1 0.830573), bal 0.819786
	data/anssel/yodaqa/large2470-training.csv MRR: 0.616968
	data/anssel/yodaqa/large2470-training.csv MAP: 0.419500
	data/anssel/yodaqa/large2470-val.csv Accuracy: raw 0.726943 (y=0 0.735079, y=1 0.599774), bal 0.667426
	data/anssel/yodaqa/large2470-val.csv MRR: 0.543145
	data/anssel/yodaqa/large2470-val.csv MAP: 0.343900
	data/anssel/yodaqa/large2470-test.csv Accuracy: raw 0.745831 (y=0 0.766308, y=1 0.522780), bal 0.644544
	data/anssel/yodaqa/large2470-test.csv MRR: 0.551201
	data/anssel/yodaqa/large2470-test.csv MAP: 0.357700
