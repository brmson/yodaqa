Concept classification using logistic regression
================================================

This script is for training and evaluating a classifier of concepts using the edit distance, dbpedia popularity, CrossWiki probability and other features.

It requires two inputs:

  * The gold standard dataset. For moviesC-train, it is located in:

	dataset-factoid-movies/moviesC/entity-linking.json

  * Question dump dataset with concept features.  First, disable the top-5
    restriction in src/main/java/cz/brmlab/yodaqa/analysis/question/CluesToConcepts.java
    (near the end of process(); XXX).  Then:

	./gradlew questionDump -PexecArgs="data/eval/moviesC-train.tsv data/ml/concepts/questionDump-tofix.json"
	python data/ml/qclass/repair-json.py data/ml/concepts/questionDump-tofix.json > data/ml/concepts/questionDump.json

To train the classifier:

	python data/ml/concepts/concepts_train_logistic.py data/ml/concepts/questionDump.json ../dataset-factoid-movies/moviesC/entity-linking.json

Put the full output in the class code of:

	src/main/java/cz/brmlab/yodaqa/analysis/question/ConceptClassifier.java

Experiments
===========
We trained and tested the classifier on moviesC-train dataset (commit hash 93a974e), taking the top5 concepts sorted by dbpedia popularity.

using the 6 features (edit distance, crosswiki prob, dbpedia popularity, isByLAT, isByNE, isBySubject), the mean precision using cross validation is 85.9% with the standard devation of 1.25%.

After we added 2 booleans (getByFuzzyLookup/getByCWLookup), the average precision is 86.9% with the standard deviation of 2.23%

by taking all concepts (instead of top5 sorted by dbpedia popularity), the precision changed to 93.2% with standard deviation of 0.91%

Further changes can be done, such as increasing the number of concepts from CW lookup.
