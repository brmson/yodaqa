#!/usr/bin/python
#
# Train a Naive Bayes classifier to predict which Freebase
# property paths would match answers given the question features.
#
# Usage: fbpath-train-bnb.py TRAIN.JSON MODEL.JSON

import json
import numpy as np
from sklearn.feature_extraction import DictVectorizer
from sklearn.multiclass import OneVsRestClassifier
from sklearn.naive_bayes import BernoulliNB
from sklearn.preprocessing import MultiLabelBinarizer
from sklearn.svm import SVC
import sys


def q_to_fdict(q):
    fdict = {}
    for lat in q['LAT']:
        fdict['lat/' + lat['text'] + '/' + lat['type']] = 1
    for sv in q['SV']:
        fdict['sv'] = sv
    return fdict


def q_to_lset(q):
    lset = set()
    for rp in q['relPaths']:
        lset.add('|'.join(rp[0]))
    return lset


class VectorizedData:
    """ Simple container that holds the input dataset
    in a sklearn-friendly form, with X, y numpy vectors.

    TODO: we ignore # of matches for each fbpath """
    def __init__(self, data, Xdict=None, Ydict=None):
        fdict = [q_to_fdict(q) for q in data]
        lset = [q_to_lset(q) for q in data]

        if Xdict is None:
            self.Xdict = DictVectorizer()
            self.X = self.Xdict.fit_transform(fdict)
        else:
            self.Xdict = Xdict
            self.X = self.Xdict.transform(fdict)

        if Ydict is None:
            self.Ydict = MultiLabelBinarizer()
            self.Y = self.Ydict.fit_transform(lset)
        else:
            self.Ydict = Ydict

            # Filter out data with unknown labels, MultiLabelBinarizer() cannot
            # handle this
            known_lset = [set([label for label in ls if label in self.Ydict.classes_]) for ls in lset]
            lset_n = sum([len(ls) for ls in lset])
            known_lset_n = sum([len(ls) for ls in known_lset])
            if known_lset_n < lset_n:
                print('dropped %d out of %d labels (not in training set)' % (lset_n - known_lset_n, lset_n))

            self.Y = self.Ydict.transform(known_lset)


if __name__ == "__main__":
    trainfile, modelfile = sys.argv[1:]
    with open(trainfile, 'r') as f:
        data = json.load(f)

    vdata = VectorizedData(data)
    #cfier = OneVsRestClassifier(BernoulliNB())
    cfier = OneVsRestClassifier(SVC(kernel='linear', probability=True))
    cfier.fit(vdata.X, vdata.Y)
    y0 = cfier.predict_proba(vdata.X[0])
    print(np.size(vdata.X), np.size(vdata.X[0]), np.size(vdata.Y), np.size(vdata.Y[0]))
    print(vdata.X[0], vdata.Y[0], cfier.predict_proba(vdata.X[0]))
    print(vdata.Xdict.inverse_transform(vdata.X[0]))
    print(vdata.Ydict.inverse_transform(np.array([vdata.Y[0]])))
    iy, maxy = max(enumerate(y0[0]), key=lambda e: e[1])
    print(vdata.Ydict.classes_[iy], iy, maxy)
    print(vdata.Xdict.inverse_transform(vdata.X[0]),
          vdata.Ydict.inverse_transform(np.array([vdata.Y[0]])))
    print(cfier.score(vdata.X, vdata.Y))
