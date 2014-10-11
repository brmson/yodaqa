#!/usr/bin/python
#
# Train an sklearn classifier on the given answer TSV dataset.
#
# Usage: answer-train.py <training-answer.tsv
#
# Currently, this script trains a logistic regression classifier.
# The final line of output contains the weight vector and intercept
# term.
#
# Training is performed by 20-round 1:1 train:test random splits
# (with question granularity), picking the model with best rate of
# including the correct answer in top 5 answers on the test set.
#
# (Note that "test set" here is picked from the same measurements
# as "train set", has nothing to do with e.g. curated-test.tsv.)
#
# N.B. scikit-learn 0.14 or later (tested with 0.15.2) is required.
#
# TODO: Make use of the question id for actual training (rewarding when
# good question ranks first or in top N).

import math
import sys
from sklearn import linear_model
import numpy as np
import numpy.random as random


num_picked = 5  # Our aim is to get our to the top N here

# Cross-validation settings
num_rounds = 10
test_portion = 1.0/2


class AnswerSet:
    """
    A set of answers pertaining a single document, i.e. from which
    top num_picked answers are selected.
    """
    def __init__(self, fv_set, class_set):
        self.fv_set = np.array(fv_set)
        self.class_set = np.array(class_set)

    def measure(self, scorer):
        # Perform the selection of top N answers within this answerset
        # according to score yielded by the @scorer callable.
        # Actually, it's more complicated. There may be many ties for the
        # third place and random selection may generate a lot of evaluation
        # noise. Therefore, we actually consider all the tied candidates,
        # but each not as a single correct answer, but only 1/n correct
        # answers (where n is number of tied answers).

        (any_picked, all_picked) = (0, 0)

        score_set = scorer(self.fv_set)
        scores = sorted(score_set, reverse=True)
        try:
            score_thres = scores[num_picked - 1]
        except IndexError:
            score_thres = scores[-1]

        true_picked = 0
        true_tiedlast = 0
        all_tiedlast = 0
        for j in range(np.size(self.class_set)):
            # print(scorer, self.fv_set[j], 'SCORE', score_set[j], score_set[j] > score_thres - 0.001)
            if score_set[j] <= score_thres - 0.0001:
                continue

            if score_set[j] <= score_thres + 0.0001:
                is_tiedlast = True
                all_tiedlast += 1
            else:
                is_tiedlast = False

            if self.class_set[j] > 0:
                if not is_tiedlast:
                    true_picked += 1
                else:
                    true_tiedlast += 1

        if true_picked > 0:
            any_picked += 1
        if true_picked == 0 and true_tiedlast > 0:
            any_picked += float(true_tiedlast) / float(all_tiedlast)
        if true_picked == 3:
            all_picked += 1

        return (any_picked, all_picked)


def load_answers(f):
    answersets = []

    labels = None

    fv_set = []
    class_set = []
    qid_last = 0
    for line in f:
        if labels is None:
            labels = line.split()[1:-1]
            continue

        # Line is qid \t f0 \t f1 \t ... \t fN \t class
        fv = [float(x) for x in line.split()]
        qid = int(fv.pop(0))
        cl = fv.pop()

        # First item is occurences; skip dummy answers representing
        # no occurences
        if fv[0 * 2] < 1.0:
            continue

        if qid != qid_last:
            if len(fv_set) > 0:
                answersets.append(AnswerSet(fv_set, class_set))
            fv_set = []
            class_set = []
            qid_last = qid

        fv_set.append(fv)
        class_set.append(cl)

    if len(fv_set) > 0:
        answersets.append(AnswerSet(fv_set, class_set))
    return (answersets, labels)


def sets_by_idx(answersets, idx_set):
    fv_set = []
    class_set = []
    for i in idx_set:
        fv_set.append(answersets[i].fv_set)
        class_set.append(answersets[i].class_set)
    return (np.vstack(fv_set), np.hstack(class_set))


def traintest(answersets):
    numrows = len(answersets)
    allidx = np.arange(numrows)
    random.shuffle(allidx)
    teststart = int((1.0 - test_portion) * numrows)
    trainidx = allidx[0:teststart]
    testidx = allidx[teststart:]

    (fv_train, class_train) = sets_by_idx(answersets, trainidx)
    (fv_test, class_test) = sets_by_idx(answersets, testidx)

    return (fv_train, class_train, trainidx, fv_test, class_test, testidx)


def measure(scorer, answersets, could_picked):
    any_picked = 0
    all_picked = 0
    for pset in answersets:
        (any_picked_1, all_picked_1) = pset.measure(scorer)
        any_picked += any_picked_1
        all_picked += all_picked_1
    any_picked = float(any_picked) / could_picked
    all_picked = float(all_picked) / could_picked
    return (any_picked, all_picked)


# AnswerScoreSimple-alike scoring for performance comparison
def simple_score(labels, fvset):
    specificity = fvset[:, labels.index('@spWordNet')]
    specificity[specificity == 0.0] = math.exp(-4)
    passage_score = fvset[:, labels.index('@passageLogScore')]
    passage_score[fvset[:, labels.index('@originDocTitle')] > 0.0] = 2
    ne_bonus = np.exp(fvset[:, labels.index('@originPsgNE')])
    score = specificity * ne_bonus * fvset[:, labels.index('@occurences')] * fvset[:, labels.index('@resultLogScore')] * passage_score
    return score


def dump_weights(weights, labels):
    for i in range(len(weights[0]) / 2):
        print('%20s % 2.4f  %20s % 2.4f' % (labels[i*2], weights[0][i*2], labels[i*2 + 1], weights[0][i*2 + 1]))


def train_model(fv_train, class_train):
    """
    Train a classifier on the given (fv_train, class_train) training data.
    Returns the classifier.
    """
    cfier = linear_model.LogisticRegression(class_weight='auto', dual=False, fit_intercept=True)
    cfier.fit(fv_train, class_train)
    return cfier


def test_model(cfier, fv_test, class_test, test_answersets):
    """
    Test a given classifier on the given (fv_test, class_test) training
    data (where the set of all answers for test questions is in
    test_answerset).  Reports the test results on stdout.  Returns a tuple
    of the "test score" (which is implementation defined below) and
    a message describing the performance in more detail.
    """
    size = np.size(class_test)
    proba = cfier.predict_proba(fv_test)
    classpred_test = (proba[:,0] < proba[:,1]).astype('float')

    # ...generating per-answer stats that aren't actually that important.
    accuracy = cfier.score(fv_test, class_test)
    tp_count = float(np.sum(np.logical_and(class_test > 0.5, classpred_test > 0.5)))
    fp_count = float(np.sum(np.logical_and(class_test < 0.5, classpred_test > 0.5)))
    fn_count = float(np.sum(np.logical_and(class_test > 0.5, classpred_test < 0.5)))
    prec = tp_count / (tp_count + fp_count)
    recall = tp_count / (tp_count + fn_count)
    f2 = 5 * (prec * recall) / (4 * prec + recall)

    classpred70_test = (proba[:,1] >= 0.7).astype('float')
    tp70_count = float(np.sum(np.logical_and(class_test > 0.5, classpred70_test > 0.5)))
    tn70_count = float(np.sum(np.logical_and(class_test > 0.5, classpred70_test < 0.5)))
    fp70_count = float(np.sum(np.logical_and(class_test < 0.5, classpred70_test > 0.5)))
    fn70_count = float(np.sum(np.logical_and(class_test > 0.5, classpred70_test < 0.5)))
    accuracy70 = (tp70_count + tn70_count) / (tp70_count + tn70_count + fp70_count + fn70_count)
    prec70 = tp70_count / (tp70_count + fp70_count)
    recall70 = tp70_count / (tp70_count + fn70_count)
    f2_70 = 5 * (prec70 * recall70) / (4 * prec70 + recall70)

    # Test the model on whole questions

    could_picked = 0
    for pset in test_answersets:
        # Could we actually pick any valid answer from this set?
        if np.sum(pset.class_set) > 0:
            could_picked += 1
    avail_to_pick = float(could_picked) / len(test_answersets)

    # Classifier score is probability of class 1
    class CfierScorer:
        def __init__(self, cfier):
            self.cfier = cfier
        def __call__(self, fvset):
            score = self.cfier.predict_proba(fvset)[:, 1]
            return score
    (cfier_any_picked, cfier_all_picked) = measure(CfierScorer(cfier), test_answersets, could_picked)

    # AnswerScoreSimple-alike scoring for performance comparison
    class SimpleScorer:
        def __init__(self, labels):
            self.labels = labels
        def __call__(self, fvset):
            score = simple_score(labels, fvset)
            return score
    (simple_any_picked, simple_all_picked) = measure(SimpleScorer(labels), test_answersets, could_picked)

    msg = "PERANS acc/prec/rcl/F2 = %.3f/%.3f/%.3f/%.3f, @70 prec/rcl/F2 = %.3f/%.3f/%.3f, PERQ avail %.3f, any good = [%.3f], simple %.3f" % \
          (accuracy, prec, recall, f2, prec70, recall70, f2_70, avail_to_pick, cfier_any_picked, simple_any_picked)

    # Our decisive factor is proportion of answers that are correctly
    # estimated as good on the 70% confidence level.
    return (cfier_any_picked, msg)


def dump_answers(cfier, fv_test, class_test):
    """
    Dump detailed decisions on the testing set on stdout.
    """
    proba = cfier.predict_proba(fv_test)
    for i in range(len(fv_test)):
        print('[%05d] %.3f %.3f %d (%d) %s' % (i, proba[i][0], proba[i][1], int(proba[i][1] > proba[i][0]), class_test[i], fv_test[i]))
        # print(list(cfier.predict_proba(fv_test)))


def cross_validate(answersets, num_rounds):
    """
    Perform num_rounds-fold cross-validation of the model, returning
    the list of scores in each fold.
    """
    scores = []
    for i in range(num_rounds):
        # Generate a random train/test set split
        (fv_train, class_train, trainidx, fv_test, class_test, testidx) = traintest(answersets)
        # print np.size(fv_train, axis=0), np.size(class_train), np.size(fv_test, axis=0), np.size(class_test)

        cfier = train_model(fv_train, class_train)

        (score, msg) = test_model(cfier, fv_test, class_test, [answersets[i] for i in testidx])
        print('(testset) ' + msg)
        scores.append(score)

    return np.array(scores)


if __name__ == "__main__":
    (answersets, labels) = load_answers(sys.stdin)
    print('%d answersets, %d answers' % (len(answersets), sum([len(aset.class_set) for aset in answersets])))

    # Cross-validation phase
    print('+ Cross-validation:')
    scores = cross_validate(answersets, num_rounds)
    print('Cross-validation score mean %.3f%% S.D. %.3f%%' % (np.mean(scores) * 100, np.std(scores) * 100))

    # Train on the complete model now
    print('+ Full training set:')
    fv_full = []
    class_full = []
    for aset in answersets:
        fv_full += list(aset.fv_set)
        class_full += list(aset.class_set)
    cfier = train_model(np.array(fv_full), np.array(class_full))

    # Report the test results - the individual accuracy metrics are obviously
    # not very meaningful as we test on the training data, but it can still
    # be informative wrt. the answerset metrics, esp. 'any good'.
    (score, msg) = test_model(cfier, fv_full, class_full, answersets)
    print("(fullset) " + msg)

    print("Full model is " + str(cfier))
    print(cfier.coef_, cfier.intercept_)

    dump_weights(cfier.coef_, labels)

    if False:
        dump_answers(cfier, fv_full, class_full)
        dump_weights(cfier.coef_, labels)
