Embedding-Based Selection
=========================

One of the answer features currently used uses a classifier based
on word embeddings to count probability of property or passage sentence
containing the correct answer.

We term this either:

  * Property selection (for structured data sources, based on the property
    label embedding)

  * Sentence selection (for unstructured data sources, "answering sentence
    selection" problem)


Property Selection
------------------

Word embeding dictionary is downloaded from our maven repository, while the
weights used are located in:

	src/main/resources/cz/brmlab/yodaqa/analysis/rdf/Mbprop.txt

To re-train the property selection model, first run:

	rm data/ml/embsel/propdata/*.txt
	./gradlew tsvgs -PexecArgs="data/eval/curated-train.tsv curated-train.tsv" -Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug -Dcz.brmlab.yodaqa.dump_property_labels=data/ml/embsel/propdata 2>&1 | tee train_embsel.log

Then, take the data from data/ml/embsel and to re-train the weights,
use the toolset in:

	https://github.com/brmson/Sentence-selection

For more information, check the README there.


Sentence Selection
------------------

We didn't get this to work in a beneficial way yet.
Work in progress.
