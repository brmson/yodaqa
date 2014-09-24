Machine Learning
================

Right now, we use a machine learning models for

  * Passage scoring during the final step of passage extraction, where we
    choose which passages to analyze in more detail to generate candidate
    answers.

  * Candidate answer scoring during the final answer choice. (TODO)

Training
--------

To train the model, we run YodaQA with the trecgs frontend like during
gold standard measurements, but passing an extra mvn commandline option

	-Dcz.brmlab.yodaqa.mltraining=1

will make YodaQA generate detailed feature vector records for training
of models in file ``training-passextract.tsv`` (in the current working
directory, typically the project root).  In "train" mode, the script
``data/eval/curated-measure.sh`` will automatically enable mltraining.

The rest is TODO for now.
