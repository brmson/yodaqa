#!/usr/bin/python
#
# Train a Naive Bayes classifier to predict which Freebase
# property paths would match answers given the question features.
#
# Usage: fbpath_train_logistic.py TRAIN.JSON MODEL.JSON

import json
import numpy as np
from fbpathtrain import VectorizedData
import random
import re
from sklearn.linear_model import LogisticRegression
from sklearn.multiclass import OneVsRestClassifier
import sys
import time


def dump_cfier(cfier, Xdict, Ydict):
    print('/// Model is %s' % (re.sub('\n\\s*', ' ', str(cfier)),))
    print('{')

    for cls, cfr in zip(cfier.classes_, cfier.estimators_):
        weights = dict()
        for feat_i in np.nonzero(cfr.coef_[0] != 0)[0]:
            weights[Xdict.feature_names_[feat_i]] = cfr.coef_[0][feat_i]
        if not weights:
            continue
        weights['_'] = cfr.intercept_[0]
        print('  "%s": %s%s' % (Ydict.classes_[cls], json.dumps(weights),
              ',' if cls != cfier.classes_[-1] else ''))

    print('}')


if __name__ == "__main__":
    trainfile, valfile = sys.argv[1:]

    # Seed always to the same number to get reproducible builds
    # TODO: Make this configurable on the command line or in the environment
    random.seed(17151713)

    print('/// The weights of individual question features for each fbpath.')
    print('/// Missing features have weight zero.  Classifiers with no features are skipped.')
    print('// These weights are output by data/ml/fbpath/fbpath-train-logistic.py as this:')
    print('//')


    ## Training

    with open(trainfile, 'r') as f:
        traindata = VectorizedData(json.load(f))
    print('// traindata: %d questions, %d features, %d fbpaths' % (
          np.size(traindata.X, axis=0), np.size(traindata.X, axis=1), np.size(traindata.Y, axis=1)))
    sys.stdout.flush()

    # class_weight='auto' produces reduced performance, val mrr 0.574 -> 0.527
    # (see the notebook)
    # We use L1 regularization mainly to minimize the output model size,
    # though it seems to yield better precision+recall too.
    t_start = time.clock()
    cfier = OneVsRestClassifier(LogisticRegression(penalty='l1'), n_jobs=4)
    cfier.fit(traindata.X, traindata.Y)
    t_end = time.clock()
    print('// training took %d seconds' % (t_end-t_start,))
    sys.stdout.flush()


    ## Benchmarking

    with open(valfile, 'r') as f:
        valdata = VectorizedData(json.load(f), traindata.Xdict, traindata.Ydict)
    print('// valdata: %d questions' % (np.size(valdata.X, axis=0),))
    sys.stdout.flush()

    val_score = valdata.cfier_score(cfier, lambda cfier, X: cfier.predict_proba(X))
    print('// val sklScore %.3f, qRecallAny %.3f, qRecallAll %.3f, pathPrec %.3f, [qScoreMRR %.3f]' % (
          val_score['sklScore'],
          val_score['qRecallAny'], val_score['qRecallAll'], val_score['pPrec'],
          val_score['qScoreMRR']))
    sys.stdout.flush()


    ## Data Dump

    dump_cfier(cfier, traindata.Xdict, traindata.Ydict)
