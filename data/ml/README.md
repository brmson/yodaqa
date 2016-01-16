Machine Learning
================

Right now, we consider machine learned models in several cases.

First, cases which are not sharing the general YodaQA training infrastructure
and are *not* covered in the rest of this document:

  * In structured search, Freebase property paths to be considered are
    generated based on some question analysis features using a separately
    pre-trained model.
    Its model data lives in ``data/ml/fbpath``, refer to the README there.

  * Candidate answer extraction based on B-I-O tagging and chunking.
    Its model data lives in ``data/ml/biocrf``, refer to the README there.

  * Question classification by logistic regression using question features.
    Its model data lives in ``data/ml/qclass``, refer to the README there.

  * Entity linking classifier for selection of concepts to link.
    Its model data lives in ``data/ml/concepts``, refer to the README there.

  * Embedding-based selection of properties and sentences, living in
    ``data/ml/embsel``, refer to the README there.

Then, we have some generic infrastructure that we describe below and use for:

  * Passage scoring during the final step of passage extraction, where we
    choose which passages to analyze in more detail to generate candidate
    answers.

  * Candidate answer scoring during the final answer choice.  (This is
    the most important model.)

In general, to train models, we first need to gather training data.
We run YodaQA with the tsvgs frontend like during gold standard measurements,
but passing extra java commandline options

	-Dcz.brmlab.yodaqa.train_passextract=training-passextract.tsv -Dcz.brmlab.yodaqa.train_answer=training-answer.tsv

will make YodaQA generate detailed feature vector records for training
of models (training-*.tsv in the current working directory, typically
the project root).  If you use the ``data/eval/curated-measure.sh``
script, you are all set - in the "train" mode, it will automatically
enable generation of training data, which is stored in ``data/ml/tsv``;
our archive of this data (to be stored in that directory) for various
commits is available at

	http://pasky.or.cz/dev/brmson/yodaqa-ml/

After this data is accumulated, the training procedure is specific
for each model.

In practice, to train + evaluate performance at once, you should simply
call the ``data/eval/train-and-eval.sh`` script, which will take care
of everything.  It will also suggest a cp command to use to update the
model with the re-trained version - commit afterwards.

Passage Extraction
------------------

We currently use a four-element FV representing number and total weight
of normal clues and "about" clues (that also match the document title,
i.e. would yield many false positives).  A passage has positive class
(1) if the answer regex matches within.

We use a Logistic Regression classifier, sorting passages by the
estimated probability of class 1.  You can invoke this classifier as:

	data/ml/passextract-train.py <training-passextract.tsv

However, there is no corresponding Java class for Logistic Regression
and we do not currently use machine learning during passage extraction
because the performance is actually worse than the PassScoreSimple;
for example:

	(testset) perpsg acc/prec/rcl/F2 = 0.657/0.052/0.411/0.172,
		per-pset avail 0.367,
		any/all good picked if avail = [0.846]/0.054,
		simple 0.850/0.050
	(array([[-0.07387773,  0.48006737,  0.45212022, -0.41486308]]), array([-0.24473122]))

I.e. whenever a matching passage can be extracted from a document,
logit classifier will put at least one such passage in top three
in 84.6% of cases, while the simple classifier will do so in 85%.

Possible reasons:
  * We optimize for individual classification, not classification
    within a set of passages of a document.
  * Too few (in addition, corellated) features.  For example, matched
    clue types (noun, noun phrase, named entity, ...) could be useful
    extra features.

Answer Scoring
--------------

We currently build a feature vector (of many features, continuously growing)
for each answer.  For each feature, the vector has three elements; @ is the
raw feature value, % is a value where mean and SD is normalized over the
answer hitlist (all scored answers for a particular question) and ! is a
value which is 1.0 if the feature has _not_ been set.  An answer has positive
class (1) if the answer regex matches it.

We use a Gradient Boosting decision forest classifier, sorting answers by the
estimated probability of class 1.  You can invoke this classifier as:

	data/ml/answer-train-gradboost.py <data/ml/tsv/training-answer-COMMIT.tsv | tee gb.txt

The Java implementation of decision forest is stored in:

	src/main/java/cz/brmlab/yodaqa/analysis/ansscore/AnswerScoreDecisionForest.java

For machine learning model development, we used the v1.0 (0ae3b79) system
on the curated dataset:

  * The CSV and TSV dumps of answer feature vectors:
    http://pasky.or.cz/dev/brmson/answer-mldata-0ae3b79.tar.xz

  * The XMI dumps of answer CASes (for re-running YodaQA with new ML model):
    http://pasky.or.cz/dev/brmson/answer-xmidump-0ae3b79.tar.xz

(but as of now, it doesn't include some important features like the question
type).

The tool `data/ml/answer-comparefv.py` can be used for comparing effect
of code changes on feacture vectors; `data/ml/answer-countfv.py` can be
used for feature occurence statistics.  These analysis tools are further
explained in data/eval/README.md.


Analysis of Decision Forests
---------------------------

Visualization of the trees

We can use script for generating pdf files with visualization of the desicion
trees contained in provided pkl file:

	mkdir output_dir
	./forest-to-pdf.py input_file.pkl output_dir

Also, we can use the Python module forest_analysis to examine various
aspects of the forest.  See the pydoc in forest_analysis.py, or you can
construct one-liners like:

	python -c 'from forest_analysis import *; import joblib; cl = joblib.load("/tmp/GBC.pkl"); print "\n".join([str(c) for c in rulechains_by_significance(cl)]);'

