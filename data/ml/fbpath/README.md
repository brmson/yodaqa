Machine Learning of Freebase Paths of Properties
================================================

Here, we seek to learn specific Freebase paths that correspond to
various things we can ask about, to improve both precision (we use
just the relevant properties as hypotheses, or add a special feature
for them) and recall (we learn to walk the RDF graph through dummy
nodes).

This work is loosely inspired by Yao, 2015: Lean Question Answering over
Freebase from Scratch (http://www.cs.jhu.edu/~xuchen/paper/scratch-qa.pdf).

TL;DR - run scripts/dump-refresh.sh in dataset-factoid-webquestions and:

	data/ml/fbpath/fbpath_retrain.sh ../dataset-factoid-webquestions $googleapikey

Data
----

We want to learn a predictor of a particular property path based on
question analysis features.  For training, we use the [WebQuestions
dataset](https://github.com/brmson/dataset-factoid-webquestions),
which should ideally have near-100% Freebase recall and already has
most questions annotated by the answer property paths.  We use the
train sub-splits (trainmodel, val, devtest) for training and development.

Get a single combined dataset with both source features (dump of
a question analysis) and target labels (computed answer property paths):

	mkdir -p data/ml/fbpath/wq-fbpath
	cd ../dataset-factoid-webquestions
	for i in trainmodel val devtest; do
		scripts/fulldata.py $i ../yodaqa/data/ml/fbpath/wq-fbpath/ main/ d-dump/ d-freebase-brp/
	done

If you want to update the source features, rerun the questionDump
as explained in d-dump/README.md; if entity linking (sets of concepts)
has changed since the last dump, you will also need to regen (in order)
d-freebase-mids, d-freebase-brp and d-freebase-brp.

### Branched fbpaths

If we want to generate dataset which contains branched fbpaths (Which means that it contains relations between
two concepts and answer), we can use files from [WebQuestions dataset](https://github.com/brmson/dataset-factoid-webquestions)
too, using d-freebase-brp in the command above as written.
The classifier training procedure outlined below is appropriate.

To disable branched fbpaths, just replace d-freebase-brp with d-freebase-rp.

Model
-----

We model probabilities of various paths based on two types of features:

  * specific text+type combinations of generated LATs
  * specific SV text

The IPython notebook ``FBPath Model Experiments.ipynb`` shows some exploration
of the dataset and models we have done.

As a baseline model, we use **multi-label logistic regression** classifier
to estimate the probabilities of specific fbpaths:

	data/ml/fbpath/fbpath_train_logistic.py data/ml/fbpath/wq-fbpath/trainmodel.json data/ml/fbpath/wq-fbpath/val.json >src/main/resources/cz/brmlab/yodaqa/analysis/rdf/FBPathLogistic.model

(XXX: you need to manually delete the trailing comma on the second to last line.)
This model is used within the YodaQA runtime in the class

	src/main/java/cz/brmlab/yodaqa/analysis/rdf/FBPathLogistic.java

TODO
----

  * Use richer question analysis representation than just LATs + SV.
    E.g. in "What countries are part of United Kingdom?", we do not carry
    over the "part of" information.

  * Use per-concept instead of per-question labels, concept-specific
    features (like NE type, categories, ...), and check for in-node
    co-occurrences.  E.g. we still fail at "who plays alan parrish in
    jumanji?" because we don't match "alan parrish" at all.

  * Explore more models, e.g. k-nn.  (See also the TODO in the ipynb.)

  * Learn a more universal mapping from words or embedded question
    representation to RDF property labels (or even just names).
    What about aligning property labels with transformed vector embedding
    of the question?
