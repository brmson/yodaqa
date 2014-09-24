Machine Learning
================

Right now, we consider machine learned models in two cases:

  * Passage scoring during the final step of passage extraction, where we
    choose which passages to analyze in more detail to generate candidate
    answers.

  * Candidate answer scoring during the final answer choice.

In general, to train models, we first need to gather training data.
We run YodaQA with the tsvgs frontend like during gold standard measurements,
but passing an extra mvn commandline option

	-Dcz.brmlab.yodaqa.mltraining=1

will make YodaQA generate detailed feature vector records for training
of models (training-*.tsv in the current working directory, typically
the project root).  If you use the ``data/eval/curated-measure.sh``
script, you are all set - in the "train" mode, it will automatically
enable mltraining.

After this data is accumulated, the training procedure is specific
for each model.

Passage Extraction
------------------

We currently use a four-elemnt FV representing number and total weight
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
  * Too few features, and they are corelated.

Answer Scoring
--------------

TODO
