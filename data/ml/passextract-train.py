#!/usr/bin/python
#
# Train an sklearn classifier on the given passextract TSV dataset.
#
# Usage: passextract-train.py <training-passextract.tsv
#
# Currently, this script trains a logistic regression classifier.
# The final line of output contains the weight vector and intercept
# term.
#
# Training is performed by 20-round 3:1 train:test random splits
# (with passageset, i.e. document, granularity), picking the model
# with best rate on the test set.
#
# (Note that "test set" here is picked from the same measurements
# as "train set", has nothing to do with e.g. curated-test.tsv.)
#
# N.B. scikit-learn 0.14 or later (tested with 0.15.2) is required.
#
# TODO: Make use of the blank lines for actual training (rewarding when
# good passages enter the num_picked filtered set).

import sys
from sklearn import linear_model
import numpy as np
import numpy.random as random


num_rounds = 20
test_portion = 1.0/4
num_picked = 3  # Keep in sync with PassFilter num-picked parameter


class PassageSet:
    """
    A set of passages pertaining a single document, i.e. from which
    top num_picked passages are selected.
    """
    def __init__(self, fv_set, class_set):
        self.fv_set = np.array(fv_set)
        self.class_set = np.array(class_set)

    def measure(self, scorer):
        # Perform the selection of top three passages within this passageset
        # according to score yielded by the @scorer callable.
        # Actually, it's more complicated. There may be many ties for the
        # third place and random selection may generate a lot of evaluation
        # noise. Therefore, we actually consider all the tied candidates.

        (any_picked, all_picked) = (0, 0)

        score_set = scorer(self.fv_set)
        scores = sorted(score_set, reverse=True)
        try:
            score_thres = scores[2]
        except IndexError:
            score_thres = scores[-1]

        true_picked = 0
        for j in range(np.size(self.class_set)):
            if score_set[j] <= score_thres - 0.001:
                continue
            if self.class_set[j] > 0:
                true_picked += 1

        if true_picked > 0:
            any_picked += 1
        if true_picked == 3:
            all_picked += 1

        return (any_picked, all_picked)


def load_passages(f):
    passagesets = []

    fv_set = []
    class_set = []
    for line in f:
        # Line is f0 \t f1 \t ... \t fN \t class
        if line == "\n":
            if len(fv_set) > 0:
                passagesets.append(PassageSet(fv_set, class_set))
            fv_set = []
            class_set = []
            continue

        fv = [float(x) for x in line.split()]
        cl = fv.pop()
        fv_set.append(fv)
        class_set.append(cl)

    if len(fv_set) > 0:
        passagesets.append(PassageSet(fv_set, class_set))
    return passagesets


def sets_by_idx(passagesets, idx_set):
    fv_set = []
    class_set = []
    for i in idx_set:
        fv_set.append(passagesets[i].fv_set)
        class_set.append(passagesets[i].class_set)
    return (np.vstack(fv_set), np.hstack(class_set))


def traintest(passagesets):
    numrows = len(passagesets)
    allidx = np.arange(numrows)
    random.shuffle(allidx)
    teststart = int((1.0 - test_portion) * numrows)
    trainidx = allidx[0:teststart]
    testidx = allidx[teststart:]

    (fv_train, class_train) = sets_by_idx(passagesets, trainidx)
    (fv_test, class_test) = sets_by_idx(passagesets, testidx)

    return (fv_train, class_train, trainidx, fv_test, class_test, testidx)


def measure(scorer, passagesets, could_picked):
    any_picked = 0
    all_picked = 0
    for pset in passagesets:
        (any_picked_1, all_picked_1) = pset.measure(scorer)
        any_picked += any_picked_1
        all_picked += all_picked_1
    any_picked = float(any_picked) / could_picked
    all_picked = float(all_picked) / could_picked
    return (any_picked, all_picked)


if __name__ == "__main__":
    passagesets = load_passages(sys.stdin)

    best = (None, -1)

    for i in range(num_rounds):
        # Generate a random train/test set split
        (fv_train, class_train, trainidx, fv_test, class_test, testidx) = traintest(passagesets)
        # print np.size(fv_train, axis=0), np.size(class_train), np.size(fv_test, axis=0), np.size(class_test)

        # Train the model

        cfier = linear_model.LogisticRegression(class_weight='auto', dual=False, fit_intercept=True, intercept_scaling=1e6)
        cfier.fit(fv_train, class_train)

        # Test the model

        size = np.size(class_test)
        proba = cfier.predict_proba(fv_test)
        classpred_test = (proba[:,0] < proba[:,1]).astype('float')

        # ...generating per-passage stats that aren't actually that important.
        accuracy = cfier.score(fv_test, class_test)
        tp_count = float(np.sum(np.logical_and(class_test > 0.5, classpred_test > 0.5)))
        fp_count = float(np.sum(np.logical_and(class_test < 0.5, classpred_test > 0.5)))
        fn_count = float(np.sum(np.logical_and(class_test > 0.5, classpred_test < 0.5)))
        prec = tp_count / (tp_count + fp_count)
        recall = tp_count / (tp_count + fn_count)
        f2 = 5 * (prec * recall) / (4 * prec + recall)

        # Test the model on whole passagesets
        test_passagesets = [passagesets[i] for i in testidx]

        could_picked = 0
        for pset in test_passagesets:
            # Could we actually pick any valid passage from this set?
            if np.sum(pset.class_set) > 0:
                could_picked += 1
        avail_to_pick = float(could_picked) / len(testidx)

        # Classifier score is probability of class 1
        class CfierScorer:
            def __init__(self, cfier):
                self.cfier = cfier
            def __call__(self, fvset):
                return self.cfier.predict_proba(fvset)[:, 1]
        (cfier_any_picked, cfier_all_picked) = measure(CfierScorer(cfier), test_passagesets, could_picked)

        # PassScoreSimple-alike scoring for performance comparison
        (simple_any_picked, simple_all_picked) = measure(lambda fvset: fvset[:, 1] + fvset[:, 3] * 0.25, test_passagesets, could_picked)

        print("(testset) perpsg acc/prec/rcl/F2 = %.3f/%.3f/%.3f/%.3f, per-pset avail %.3f, any/all good picked if avail = [%.3f]/%.3f, simple %.3f/%.3f" %
              (accuracy, prec, recall, f2, avail_to_pick, cfier_any_picked, cfier_all_picked, simple_any_picked, simple_all_picked))

        # Our decisive factor is proportion of passagesets where at least
        # one picked passage contains the answer.  FIXME: Also actually
        # try to maximize that in the predictor.
        score = cfier_any_picked
        if score > best[1]:
            best = (cfier, score, fv_test, class_test)

    print("Best is " + str(best))
    print(best[0].coef_, best[0].intercept_)

    if False:
        (cfier, _, fv_test, class_test) = best
        proba = cfier.predict_proba(fv_test)
        for i in range(len(fv_test)):
            print('[%05d] %.3f %.3f %d (%d) %s' % (i, proba[i][0], proba[i][1], int(proba[i][1] > proba[i][0]), class_test[i], fv_test[i]))
            # print(list(cfier.predict_proba(fv_test)))
        print(best[0].coef_, best[0].intercept_)
