Machine Learning
================

Right now, we consider machine learned models in two cases:

  * Passage scoring during the final step of passage extraction, where we
    choose which passages to analyze in more detail to generate candidate
    answers.

  * Candidate answer scoring during the final answer choice.

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

We use a Logistic Regression classifier, sorting answers by the
estimated probability of class 1.  You can invoke this classifier as:

	data/ml/answer-train.py <data/ml/tsv/training-answer-COMMIT.tsv | tee logistic.txt

The Java implementation of logistic classifier is stored in

	src/main/java/cz/brmlab/yodaqa/analysis/answer/AnswerScoreLogistic.java

and to update it based on the regression training run, edit that file
and paste the contents of logistic.txt at the marked location near the
top (replacing previous content).

(There is also a AnswerScoreSimple scorer which uses just a small set of
features and is super-simplistic, what we used before; its performance is
contrasted by the 'simple' performance rate output by answer-train.py.)

The tool `data/ml/answer-comparefv.py` can be used for comparing effect
of code changes on feacture vectors, and is further explained in
data/eval/README.md.
