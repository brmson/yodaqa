  * sample.xml: Few hand-picked questions from qald-5_train for initial
    development and testing.

  * qald-5_train.xml: The master training dataset, to be downloaded from
	http://greententacle.techfak.uni-bielefeld.de/~cunger/qald/index.php?x=challenge&q=5
  * qald5-train.xml: Training set for development
  * qald5-test.xml: Testing set for development
  * qald-5_test_questions.xml: Validation dataset, released without
    gold standard for the competition, to be downloaded from
	http://greententacle.techfak.uni-bielefeld.de/~cunger/qald/index.php?x=challenge&q=5


qald5-train.xml, qald5-test.xml is generated from qald-5_train.xml using

	traintest-shuffle.pl qald-5_train.xml 170 qald5-train.xml 170 qald5-test.xml

i.e. as a randomized 1:1 split of the master training dataset.  It also
omits the questions marked as "OUT OF SCOPE".
