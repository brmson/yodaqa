Concept classification using logistic regression
================================================

This script is for training and evaluating a classifier of concepts using the edit distance, dbpedia popularity, CrossWiki probability and other features.

TL;DR:

	data/ml/concepts/concepts_retrain.sh ../dataset-factoid-movies/moviesF

Retraining
----------

It requires two inputs:

  * The gold standard dataset. For moviesF-train, it is located in:

	dataset-factoid-movies/moviesF/entity-linking.json

  * Question dump dataset with concept features (*all*, not just top N as is usual).

	./gradlew questionDump -PexecArgs="data/eval/moviesF-train.tsv data/ml/concepts/questionDump-tofix.json" -Dcz.brmlab.yodaqa.topLinkedConcepts=0
	python data/ml/repair-json.py data/ml/concepts/questionDump-tofix.json > data/ml/concepts/questionDump.json

To train the classifier:

	python data/ml/concepts/concepts_train_logistic.py data/ml/concepts/questionDump.json ../dataset-factoid-movies/moviesF/entity-linking.json

Put the full output in the class code of:

	src/main/java/cz/brmlab/yodaqa/analysis/question/ConceptClassifier.java

Experiments (archive)
---------------------

We trained and tested the classifier on moviesC-train dataset (commit hash 93a974e), taking the top5 concepts sorted by dbpedia popularity.

using the 6 features (edit distance, crosswiki prob, dbpedia popularity, isByLAT, isByNE, isBySubject), the mean precision using cross validation is 85.9% with the standard devation of 1.25%.

After we added 2 booleans (getByFuzzyLookup/getByCWLookup), the average precision is 86.9% with the standard deviation of 2.23%

by taking all concepts (instead of top5 sorted by dbpedia popularity), the precision changed to 93.2% with standard deviation of 0.91%

Further changes can be done, such as increasing the number of concepts from CW lookup.
