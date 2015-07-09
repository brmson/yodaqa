Machine Learning of Freebase Paths of Properties
================================================

Here, we seek to learn specific Freebase paths that correspond to
various things we can ask about, to improve both precision (we use
just the relevant properties as hypotheses, or add a special feature
for them) and recall (we learn to walk the RDF graph through dummy
nodes).

Data
----

We want to learn a predictor of a particular property path based on
question analysis features.  For training, we use the [WebQuestions
dataset](https://github.com/brmson/dataset-factoid-webquestions),
which should ideally have near-100% Freebase recall and already has
most questions annotated by the answer property paths.  We use the
train sub-splits (trainmodel, val, devtest) for training and development.

To generate a JSON dump of question analysis of webquestions, use:

	mkdir data/ml/fbpath/wq-qdump
	for i in trainmodel val devtest; do
		./gradlew questionDump -PexecArgs="../dataset-factoid-webquestions/tsv/$i.tsv data/ml/fbpath/wq-qdump/${i}.json"; done
	done

(You need to edit the generated files to make JSON arrays - add [ to
the beginning, ] to the end, and a comma after each but the last record.)
This is fast, just a few minutes for full training set.  Still, you
can get it for YodaQA 0dc0c0f, dataset-factoid-webquestions 4cf3d15
at: http://pasky.or.cz/dev/brmson/wq-qdump/

Combine to a single dataset:

	mkdir data/ml/fbpath/wq-fbpath
	cd ../dataset-factoid-webquestions
	for i in trainmodel val devtest; do
		scripts/fulldata.py $i ../yodaqa/data/ml/fbpath/wq-fbpath/ main/ d-freebase-rp/ ../yodaqa/data/ml/fbpath/wq-qdump/
	done

To get the dataset at this stage: http://pasky.or.cz/dev/brmson/wq-fbpath/

Model
-----

We model probabilities of various paths based on two types of features:

  * specific text+type combinations of generated LATs
  * specific SV text

Train a proof-of-concept BernoulliNB model with

	data/ml/fbpath/fbpath_train_bnb.py train.json bayesmodel.json

XXX trainmodel, val, devtest?

TODO
----

  * Better model scoring function.

  * Search for best model.

  * Use richer question analysis representation than just LATs + SV.

  * Learn a more universal mapping from words or embedded question
    representation to RDF property labels (or even just names).
