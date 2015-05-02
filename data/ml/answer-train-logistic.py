#!/usr/bin/python -u
#
# Train a LogisticRegression classifier on the given answer TSV dataset.
#
# Usage: answer-train-logistic.py MODELPARAM... <training-answer.tsv
#
# MODELPARAM can be argument of GradientBoostingClassifier(), or
# * base_class_ratio, controlling class balance (1 is fully balanced)
# * exclude, list-syntax listing of feature label regexes to exclude
#   from training, e.g. "exclude=['.simpleScore','\!.*']"
#
# Currently, this script trains a logistic regression classifier.
# The output is a java code with the classifier configuration, to be
# pasted into:
# src/main/java/cz/brmlab/yodaqa/analysis/answer/AnswerScoreLogistic.java
#
# N.B. scikit-learn 0.14 or later (tested with 0.15.2) is required.
#
# TODO: Make use of the question id for actual training (rewarding when
# good question ranks first or in top N).

import sys
import time
from sklearn import linear_model
import numpy as np
import numpy.random as random

from answertrain import *


def dump_weights(weights, labels):
    for i in range(len(weights[0]) / 3):
        print(' * %28s % 2.4f  %28s % 2.4f  %28s % 2.4f' %
              (labels[i*3], weights[0][i*3],
               labels[i*3 + 1], weights[0][i*3 + 1],
               labels[i*3 + 2], weights[0][i*3 + 2]))


def dump_model(weights, labels, intercept):
    for i in range(len(weights[0]) / 3):
        # d01 is roughly estimated delta between feature not present and
        # feature present and set to 1 - basically, the baseline influence
        # of the feature (it has some meaning even for non-binary features)
        d01 = weights[0][i*3] + weights[0][i*3 + 1] - weights[0][i*3 + 2]
        print('\t/* %27s @,%%,! */ % 2.6f, % 2.6f, % 2.6f, /* %27s d01: % 2.6f */' %
              (labels[i*3][1:],
               weights[0][i*3], weights[0][i*3 + 1], weights[0][i*3 + 2],
               labels[i*3][1:], d01))
    print('/* intercept */ %f' % intercept)


class GBFactory:
    def __init__(self, cfier_params):
        self.cfier_params = cfier_params
        self.base_class_ratio = self.cfier_params.pop('base_class_ratio', 0.5)
        self.fit_intercept = self.cfier_params.pop('fit_intercept', True)

    def __call__(self, class_ratio, fv_train, class_train):
        cfier = linear_model.LogisticRegression(
                    class_weight={0: 1, 1: self.base_class_ratio/class_ratio},
                    dual=False, fit_intercept=self.fit_intercept, **self.cfier_params)
        cfier.fit(fv_train, class_train)
        return cfier


if __name__ == "__main__":
    # Seed always to the same number to get reproducible builds
    # TODO: Make this configurable on the command line or in the environment
    random.seed(17151713)

    modelparams = sys.argv[1:]
    cfier_opts = dict()
    for p in modelparams:
        k, v = p.split('=')
        cfier_opts[k] = eval(v)
    cfier_factory = GBFactory(cfier_opts)

    exclude_labels = cfier_opts.pop('exclude', [])
    (answersets, labels) = load_answers(sys.stdin, exclude_labels)

    print('/// The weights of individual elements of the FV.  These weights')
    print('// are output by data/ml/answer-train-logistic.py as this:')
    print('//')
    print('// %d answersets, %d answers' % (len(answersets), sum([len(aset.class_set) for aset in answersets])))

    # Cross-validation phase
    print('// + Cross-validation:')
    scores = cross_validate(answersets, labels, cfier_factory)
    print('// (mean) ' + test_msg(*list(np.mean(scores, axis=0))))
    print('// (S.D.) ' + test_msg(*list(np.std(scores, axis=0))))

    # Train on the complete model now
    print('// + Full training set:')
    fv_full, class_full = fullset(answersets)
    t_start = time.clock()
    cfier = train_model(fv_full, class_full, cfier_factory)
    t_end = time.clock()
    print('// training took %d seconds' % (t_end-t_start,))

    # Report the test results - the individual accuracy metrics are obviously
    # not very meaningful as we test on the training data, but it can still
    # be informative wrt. the answerset metrics, esp. 'any good'.
    testres = test_model(cfier, fv_full, class_full, answersets, labels)
    print("// (full) " + test_msg(*testres))

    # dump_weights(cfier.coef_, labels)

    print("// Full model is " + str(cfier).replace("\n", " "))
    print('//')
    dump_model(cfier.coef_, labels, cfier.intercept_)

    if False:
        dump_answers(cfier, fv_full, class_full)
        dump_weights(cfier.coef_, labels)
