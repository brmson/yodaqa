Concept classification using logistic regression
================================================

This script is for training and evaluating a classifier of concepts using the edit distance, dbpedia popularity, CrossWiki probability and other features.

It requires the gold standard dataset. For moviesC-train, it is located in dataset-factoid-movies/moviesC/entity-linking.json

First start repair-json.py from data/ml/qclass

    python ../qclass/repair-json.py questionDump.py > questionDump_fixed.json

Then start the script:

    python concepts_train_logistic.py questionDump_fixed.json entity-linking.json

Experiments
===========
We trained and tested the classifier on moviesC-train dataset (commit hash 93a974e), taking the top5 concepts sorted by dbpedia popularity.

using the 6 features (edit distance, crosswiki prob, dbpedia popularity, isByLAT, isByNE, isBySubject), the mean precision using cross validation is 85.9% with the standard devation of 1.25%.

After we added 2 booleans (getByFuzzyLookup/getByCWLookup), the average precision is 86.9% with the standard deviation of 2.23%

by taking all concepts (instead of top5 sorted by dbpedia popularity), the precision changed to 93.2% with standard deviation of 0.91%

Further changes can be done, such as increasing the number of concepts from CW lookup.