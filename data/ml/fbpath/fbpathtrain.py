"""
Service routines for training a Naive Bayes classifier to predict which
Freebase property paths would match answers given the question features.
"""

from __future__ import print_function

import numpy as np
from sklearn.feature_extraction import DictVectorizer
from sklearn.preprocessing import MultiLabelBinarizer
import sys


def q_to_fdict(q):
    fdict = {}

    for lat in q['LAT']:
        fdict['lat/' + lat['text'] + '/' + lat['type']] = 1

    fdict['sv'] = q['SV'][0] if q['SV'] else ''
    fdict['lsv'] = q['lemmaSV'][0] if q['lemmaSV'] else ''

    # prefer shortest NP as feature
    subjnp = sorted([s['text'] for s in q['Subject'] if s['type'] == 'NP'], key=lambda t: len(t))
    subjtok = [s['text'] for s in q['Subject'] if s['type'] == 'Token']
    fdict['subjnp'] = subjnp[0] if subjnp else ''
    fdict['subjtok'] = subjtok[0] if subjtok else ''

    return fdict


def q_to_lset(q):
    lset = set()
    for rp in q['relPaths']:
        lset.add('|'.join(rp[0]))
    return lset


def mrr_by_score(Y, Yscores):
    recipr_ranks = []
    for i in range(np.size(Y, axis=0)):
        pathj_by_score = [k[0] for k in sorted(enumerate(Yscores[i]), key=lambda k: k[1], reverse=True)]
        n_j = 0
        rank = None
        for j in pathj_by_score:
            if Y[i][j] == 1:
                rank = n_j+1
                break
            n_j += 1
        if rank is not None:
            recipr_ranks.append(1/float(rank))
        else:
            # we are interested in MRR just for questions that *have* a solution
            pass
            # recipr_ranks.append(0)
    return np.mean(recipr_ranks)


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
                print('dropped %d out of %d labels (not in training set)' % (lset_n - known_lset_n, lset_n), file=sys.stderr)

            self.Y = self.Ydict.transform(known_lset)

    def cfier_score(self, cfier, scorer):
        """ Measure cfier performance on this dataset.

        scorer -> lambda cfier, X: cfier.predict_proba(X)
        (or decision_function when probabilities not predicted) """
        skl_score = cfier.score(self.X.toarray(), self.Y)

        # XXX: Matched paths might/could be weighted by their nMatches too...

        # Measure prediction performance
        Ypred = cfier.predict(self.X.toarray())
        n_q = float(np.size(self.Y, axis=0))
        # number of questions where all correct paths have been recalled
        recall_all = np.sum(np.sum(self.Y, axis=1) == np.sum(Ypred * self.Y, axis=1)) / n_q
        # number of questions where at least one correct path has been recalled
        recall_any = np.sum((np.sum(self.Y, axis=1) != 0) == (np.sum(Ypred * self.Y, axis=1) != 0)) / n_q
        # number of *PATHS* (not q.) that were correct
        precision = np.sum((Ypred + self.Y) == 2) / float(np.sum(Ypred))

        # Measure scoring performance
        Yscores = scorer(cfier, self.X.toarray())
        # MRR of first correct path
        mrr = mrr_by_score(self.Y, Yscores)
        # number of questions where at least one correct path has been recalled in top N paths
        # TODO

        return {'sklScore': skl_score, 'qRecallAll': recall_all, 'qRecallAny': recall_any, 'pPrec': precision, 'qScoreMRR': mrr}
