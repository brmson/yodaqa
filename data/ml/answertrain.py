"""
Generic framework for training answer classifiers using sklearn
on an answer TSV dataset.

This module contains a generic train / test function and cross-validation
routine, but does not define the actual classifier to use; it is expected
that the calling scripts will provide these.
"""

import math
from multiprocessing import Pool
import numpy as np
import numpy.random as random
import os
import re


num_picked = 5  # Our aim is to get our to the top N here

# Cross-validation settings
num_rounds = 10
test_portion = 1.0/2


class AnswerSet:
    """
    A set of answers pertaining a single question, i.e. from which
    top num_picked answers are selected.
    """
    def __init__(self, fv_set, class_set):
        # Sort the vectors (YodaQA output order is unstable),
        # then shuffle (to randomize).  This ensures that
        # vectors go in randomized, but reproducible across runs.
        fv_set = np.array(fv_set)
        class_set = np.array(class_set)

        order = np.lexsort(np.hstack([fv_set, np.array(class_set, ndmin=2).T]).T)
        z = zip(fv_set[order], class_set[order])
        random.shuffle(z)

        self.fv_set = np.array([i[0] for i in z])
        self.class_set = np.array([i[1] for i in z])

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
        if true_picked == num_picked:
            # XXX: all_picked doesn't really make sense; it would be more
            # interesting to separately trace the top answer class
            all_picked += 1

        # Compute MRR
        mrr = 0
        scores_classes = zip(list(score_set), list(self.class_set))
        rank = 1
        next_rank = rank
        last_s = 1e10
        for s, c in sorted(scores_classes, key=lambda k: k[0], reverse=True):
            if s < last_s - 0.0001:
                rank = next_rank
            last_s = s
            if c > 0:
                mrr = 1.0 / rank
                break
            next_rank += 1

        return (any_picked, all_picked, mrr)


def fi_by_label(labels, regex):
    """ Return feature index by (whole-)label regex """
    for i in range(len(labels)):
        if re.match('^'+regex+'$', labels[i]):
            yield i


def load_answers(f, exclude=[]):
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
        items = line.split()
        qid = items.pop(0)
        cl = int(items.pop())
        fv = [float(x) for x in items]

        # First item is occurences; skip dummy answers representing
        # no occurences
        if fv[0 * 2] < 1.0:
            continue

        for labelre in exclude:
            for i in fi_by_label(labels, labelre):
                fv[i] = 0

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
    """ Create a random split of answersets to training and test set
    in the (fv, class) format. """
    numrows = len(answersets)
    allidx = np.arange(numrows)
    random.shuffle(allidx)
    teststart = int((1.0 - test_portion) * numrows)
    trainidx = allidx[0:teststart]
    testidx = allidx[teststart:]

    (fv_train, class_train) = sets_by_idx(answersets, trainidx)
    (fv_test, class_test) = sets_by_idx(answersets, testidx)

    return (fv_train, class_train, trainidx, fv_test, class_test, testidx)


def fullset(answersets):
    """ Return the full answersets as a (fv, class) format set. """
    fv_full = []
    class_full = []
    for aset in answersets:
        fv_full += list(aset.fv_set)
        class_full += list(aset.class_set)
    return np.array(fv_full), np.array(class_full)


def measure(scorer, answersets, could_picked):
    any_picked = 0
    all_picked = 0
    mrr = 0
    for pset in answersets:
        (any_picked_1, all_picked_1, mrr_1) = pset.measure(scorer)
        any_picked += any_picked_1
        all_picked += all_picked_1
        mrr += mrr_1
    any_picked = float(any_picked) / could_picked
    all_picked = float(all_picked) / could_picked
    mrr = float(mrr) / could_picked
    return (any_picked, all_picked, mrr)


# AnswerScoreSimple-alike scoring for performance comparison
def simple_score(labels, fvset):
    specificity = np.array(fvset[:, labels.index('@spWordNet')])
    specificity[specificity == 0.0] = math.exp(-4)
    passage_score = np.array(fvset[:, labels.index('@passageLogScore')])
    passage_score[fvset[:, labels.index('@originDocTitle')] > 0.0] = 2
    ne_bonus = np.exp(fvset[:, labels.index('@originPsgNE')])
    score = specificity * ne_bonus * fvset[:, labels.index('@occurences')] * fvset[:, labels.index('@resultLogScore')] * passage_score
    return score


def train_model(fv_train, class_train, cfier_factory):
    """
    Train a classifier on the given (fv_train, class_train) training data.
    Returns the classifier.

    The classifier is built and trained by calling cfier_factory(class_ratio).
    """
    class_ratio = float(np.sum(class_train == 1)) / np.size(class_train)
    # print('// class ratio ', class_ratio)
    cfier = cfier_factory(class_ratio, fv_train, class_train)
    return cfier


def test_model(cfier, fv_test, class_test, test_answersets, labels):
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
    try:
        prec = tp_count / (tp_count + fp_count)
    except ZeroDivisionError:
        prec = 0
    try:
        recall = tp_count / (tp_count + fn_count)
    except ZeroDivisionError:
        recall = 0
    try:
        f1 = 2 * (prec * recall) / (prec + recall)
    except ZeroDivisionError:
        f1 = 0

    classpred70_test = (proba[:,1] >= 0.7).astype('float')
    tp70_count = float(np.sum(np.logical_and(class_test > 0.5, classpred70_test > 0.5)))
    tn70_count = float(np.sum(np.logical_and(class_test > 0.5, classpred70_test < 0.5)))
    fp70_count = float(np.sum(np.logical_and(class_test < 0.5, classpred70_test > 0.5)))
    fn70_count = float(np.sum(np.logical_and(class_test > 0.5, classpred70_test < 0.5)))
    accuracy70 = (tp70_count + tn70_count) / (tp70_count + tn70_count + fp70_count + fn70_count)
    try:
        prec70 = tp70_count / (tp70_count + fp70_count)
    except ZeroDivisionError:
        prec70 = 0
    try:
        recall70 = tp70_count / (tp70_count + fn70_count)
    except ZeroDivisionError:
        recall70 = 0
    try:
        f1_70 = 2 * (prec70 * recall70) / (prec70 + recall70)
    except ZeroDivisionError:
        f1_70 = 0

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
    (cfier_any_picked, cfier_all_picked, cfier_mrr) = measure(CfierScorer(cfier), test_answersets, could_picked)

    # AnswerScoreSimple-alike scoring for performance comparison
    class SimpleScorer:
        def __init__(self, labels):
            self.labels = labels
        def __call__(self, fvset):
            score = simple_score(labels, fvset)
            return score
    (simple_any_picked, simple_all_picked, simple_mrr) = measure(SimpleScorer(labels), test_answersets, could_picked)

    return (accuracy, prec, recall, f1, prec70, recall70, f1_70, avail_to_pick, cfier_any_picked, cfier_mrr)


def test_msg(accuracy, prec, recall, f1, prec70, recall70, f1_70, avail_to_pick, cfier_any_picked, cfier_mrr):
    return "PERANS acc/prec/rcl/F1 = %.3f/%.3f/%.3f/%.3f, @70 prec/rcl/F1 = %.3f/%.3f/%.3f, PERQ avail %.3f, any good = [%.3f] MRR %.3f" % \
           (accuracy, prec, recall, f1, prec70, recall70, f1_70, avail_to_pick, cfier_any_picked, cfier_mrr)


def dump_answers(cfier, fv_test, class_test):
    """
    Dump detailed decisions on the testing set on stdout.
    """
    proba = cfier.predict_proba(fv_test)
    for i in range(len(fv_test)):
        print('[%05d] %.3f %.3f %d (%d) %s' % (i, proba[i][0], proba[i][1], int(proba[i][1] > proba[i][0]), class_test[i], fv_test[i]))
        # print(list(cfier.predict_proba(fv_test)))


def cross_validate_one(idx):
    global _g_cv_data
    (answersets, labels, cfier_factory, base_seed) = _g_cv_data
    # Make sure each worker has a different random seed
    random.seed(base_seed + idx)
    # Generate a random train/test set split
    (fv_train, class_train, trainidx, fv_test, class_test, testidx) = traintest(answersets)
    # print np.size(fv_train, axis=0), np.size(class_train), np.size(fv_test, axis=0), np.size(class_test)

    cfier = train_model(fv_train, class_train, cfier_factory)

    return test_model(cfier, fv_test, class_test, [answersets[i] for i in testidx], labels)


def cross_validate(answersets, labels, cfier_factory, num_rounds=num_rounds):
    """
    Perform num_rounds-fold cross-validation of the model, returning
    the list of test scores in each fold.
    """

    # Do not pass cv_data as parameters as that'll create a separate copy
    # for each sub-process, dramatically increasing memory improvements;
    # 16GB RAM is not enough for 8-thread cross-validation on large2180.
    global _g_cv_data
    _g_cv_data = (answersets, labels, cfier_factory, random.randint(0,2**31))

    processes = os.environ.get('ANSWERTRAIN_N_THREADS',
                os.environ.get('YODAQA_N_THREADS', None))
    if processes is not None:  processes = int(processes)
    pool = Pool(processes=processes)

    scores = []
    for res in pool.imap(cross_validate_one, range(num_rounds)):
        print('// (test) ' + test_msg(*res))
        scores.append(list(res))
    pool.close()

    return np.array(scores)
