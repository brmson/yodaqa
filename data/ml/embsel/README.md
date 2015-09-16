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

Word embeding dictionary is downloaded from our maven repository, while the
weights used are located in:

	src/main/resources/cz/brmlab/yodaqa/analysis/rdf/Mbprop.txt

The weights are trained using the toolset in:

	https://github.com/brmson/Sentence-selection

For more information, check the README there.
