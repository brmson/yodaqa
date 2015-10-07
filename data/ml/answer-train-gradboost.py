#!/usr/bin/python -u
#
# Train a GradientBoostingClassifier on the given answer TSV dataset.
#
# Usage: answer-train-gradboost.py MODELPARAM... <training-answer.tsv
#
# MODELPARAM can be argument of GradientBoostingClassifier(), or
# * base_class_ratio, controlling class balance (1 is fully balanced)
# * exclude, list-syntax listing of feature label regexes to exclude
#   from training, e.g. "exclude=['.simpleScore','\!.*']"

import sys
import time
from sklearn import ensemble
from sklearn.utils.class_weight import compute_sample_weight
import joblib
import numpy as np
import numpy.random as random
import json
import random

from answertrain import *


def dump_model(cfier, labels):
    print('{')
    print('  "prior": %f,' % (cfier.init_.prior,))
    print('  "learning_rate": %f,' % (cfier.learning_rate,))
    print('  "labels": %s,' % (json.dumps(labels),))
    print('  "forest": [')
    for tree in cfier.estimators_:
        line = dict()
        line["children_left"] = tree[0].tree_.children_left.tolist()
        line["children_right"] = tree[0].tree_.children_right.tolist()
        line["features"] = tree[0].tree_.feature.tolist()
        line["thresholds"] = [float('%.4f' % (x,)) for x in tree[0].tree_.threshold.tolist()]
        line["values"] = [float('%.4f' % (x[0][0],)) for x in tree[0].tree_.value]
        print('    %s%s' % (json.dumps(line, sort_keys=True), ',' if tree != cfier.estimators_[-1] else ''))

    print('  ]')
    print('}')


class GBFactory:
    def __init__(self, cfier_params):
        self.cfier_params = cfier_params
        self.base_class_ratio = self.cfier_params.pop('base_class_ratio', 0.5)

    def __call__(self, class_ratio, fv_train, class_train):
        rs = random.randint(0,2**31)
        cfier = ensemble.GradientBoostingClassifier(random_state=rs, **self.cfier_params)
        sample_weight = compute_sample_weight({0: 1, 1: self.base_class_ratio/class_ratio}, class_train)
        cfier.fit(fv_train, class_train, sample_weight=sample_weight)
        return cfier


if __name__ == "__main__":
    # Seed always to the same number to get reproducible builds
    # TODO: Make this configurable on the command line or in the environment
    random.seed(17151713)
    np.random.seed(random.randint(0,2**31))

    modelparams = sys.argv[1:]
    if (len(modelparams) == 0):
        modelparams = ['n_estimators=200', 'max_leaf_nodes=10', 'min_samples_split=10', 'min_samples_leaf=5', "max_features='sqrt'"]
    cfier_opts = dict()
    for p in modelparams:
        k, v = p.split('=')
        cfier_opts[k] = eval(v)
    cfier_factory = GBFactory(cfier_opts)

    exclude_labels = cfier_opts.pop('exclude', [])
    (answersets, labels) = load_answers(sys.stdin, exclude_labels)

    print('/// The weights of individual elements of the FV.  These weights')
    print('// are output by data/ml/answer-train-gradboost.py as this:')
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

    # Store the model
    if False:
        modelfile = ('GradientBoostingClassifier-%s.pkl' % (','.join(modelparams),)).replace('/', '%')
        joblib.dump((cfier, labels), modelfile, compress=3)
        print('// model file ' + modelfile)

    # Report the test results - the individual accuracy metrics are obviously
    # not very meaningful as we test on the training data, but it can still
    # be informative wrt. the answerset metrics, esp. 'any good'.
    testres = test_model(cfier, fv_full, class_full, answersets, labels)
    print("// (full) " + test_msg(*testres))

    print("// Full model is " + str(cfier).replace("\n", " "))
    print('//')
    dump_model(cfier, labels)
