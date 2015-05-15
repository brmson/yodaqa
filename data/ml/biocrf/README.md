The biocrf model is used by the biotagger passage analysis (candidate answer
generator) engine.

To re-train the model, run:

	./gradlew tsvgs -PexecArgs="data/eval/curated-train.tsv curated-train.tsv" -Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug -Dcz.brmlab.yodaqa.train_ansbiocrf=1 2>&1 | tee train_ansbiocrf.log
	./gradlew biocrftrain
