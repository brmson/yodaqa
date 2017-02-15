Freebase-path classification 
============================

These scripts could be used for embedding based classification of freebase-paths.	
Make sure your working directory is set to yodaqa root before running any script.

Required data
-------------

  * Checkout of https://github.com/brmson/Sentence-selection in parent direcotry of yodaqa root

  *	Checkout of https://github.com/brmson/dataset-factoid-webquestions in parent directory of yodaqa root

  * Relation neighbourhood of every concept in training dataset:

  	```
	mkdir -p data/ml/fbpath-emb/relations/concept-relations
	mkdir -p data/ml/fbpath-emb/relations2/concept-relations
	data/ml/fbpath-emb/generate_relations.py 1 trainmodel ../dataset-factoid-webquestions/d-dump/ data/ml/fbpath-emb/relations/
	data/ml/fbpath-emb/generate_relations.py 2 trainmodel ../dataset-factoid-webquestions/d-dump/ ../dataset-factoid-webquestions/d-freebase-brp/ data/ml/fbpath-emb/relations2/
	```

Retraining
----------

For retraining the matrices use following script:

	data/ml/fbpath-emb/fbpath_retrain.sh ../dataset-factoid-webquestions/
