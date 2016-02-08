#!/usr/bin/python
#
# concepts_train_logistic - train a logistic regression classifier of concept
# relevance
#
# Example: data/ml/concepts/concepts_train_logistic.py data/ml/concepts/questionDump.json ../dataset-factoid-movies/moviesF/entity-linking.json
#
# The output is to be pasted in
# src/main/java/cz/brmlab/yodaqa/analysis/question/ConceptClassifier.java
# (indented).

from __future__ import print_function
from __future__ import division
import sys
import json
import random
from sklearn import linear_model

# the set of input features; the labels match Concept attributes in questionDump
feats = ['editDist', 'labelProbability', 'logPopularity', 'relatedness', 'getByLAT', 'getByNE', 'getBySubject', 'getByNgram', 'getByFuzzyLookup', 'getByCWLookup']

# cross validation parameters
num_rounds = 10
test_portion = 1.0 / 5


class Concept:
    """ Concept with a name (the string), fv (feature vector)
    and a label (1/0 relevance tag, don't confuse with name!) """
    def __init__(self, name, fv, label):
        self.name = name
        self.fv = fv
        self.label = label

    @staticmethod
    def from_q(qconc, is_valid):
        return Concept(qconc['fullLabel'], [float(qconc[f]) for f in feats], int(is_valid))


def load(input_list, gold_standard):
    concepts = []
    correct_counter = 0
    incorrect_counter = 0
    for q, q_gs in zip(input_list, gold_standard):
        assert q['qId'] == q_gs['qId']
        for conc in q['Concept']:
            valid = False
            for corr in q_gs['Concept']:
                if conc['fullLabel'] == corr['fullLabel']:
                    valid = True
                    break
            if valid is True:
                correct_counter += 1
            else:
                incorrect_counter += 1
            concepts.append(Concept.from_q(conc, valid))
    print("/* Training data - correct: %d (%.3f%%), incorrect: %d (%.3f%%) */" % (
        correct_counter, correct_counter / len(concepts) * 100,
        incorrect_counter, incorrect_counter / len(concepts) * 100))
    return concepts


def train_model(concepts):
    clf = linear_model.LogisticRegression(C=1, penalty='l2', tol=1e-5)
    clf.fit([c.fv for c in concepts], [c.label for c in concepts])
    return clf


def test_model(desc, concepts_test, cfier):
    proba = cfier.predict_proba([c.fv for c in concepts_test])
    corr = 0
    incorr = 0
    total = len(concepts_test)
    for concept, p in zip(concepts_test, proba):
        # if desc == 'Training set':
        #     print("% -40s =%d %.3f :: %s" % (concept.name, concept.label, p[0], concept.fv))
        if p[0] > 0.5:
            prediction = 0
        else:
            prediction = 1
        if prediction == concept.label:
            corr += 1
        else:
            incorr += 1
    stat = corr / total * 100
    print("/* %s precision %.3f%% (%d/%d) */" % (desc, stat, corr, total))
    return (corr, incorr, total)


def dump_model(clf):
    print("/* Model (trained on the whole training set): */")
    print("double[] weights = {")
    for i in range(len(feats)):
        print("\t%f, // %s" % (classifier.coef_[0][i], feats[i]))
    print("};")
    print("double intercept = %f;" % (classifier.intercept_,))


def split_dataset(concepts):
    """
    Create a random split of (fv, labels) tuple list to training and test set
    """
    newlist = list(concepts)
    random.shuffle(newlist)

    teststart = int((1.0 - test_portion) * len(newlist))
    train_list = newlist[0:teststart]
    test_list = newlist[teststart:]
    return (train_list, test_list)


def cross_validate(concepts):
    (concepts_train, concepts_test) = split_dataset(concepts)
    classifier = train_model(concepts_train)
    return test_model('CV fold', concepts_test, classifier)


def cross_validate_all(concepts):
    random.seed(1234567)
    total = 0
    correct = 0
    mean = 0
    res = []
    for _ in range(num_rounds):
        (c, i, t) = cross_validate(concepts)
        res.append((c, i, t))
        correct += c
    total = t
    mean = correct / num_rounds
    x_sum = 0
    for triplet in res:
        x_sum += (triplet[0] - mean) ** 2
    variance = (x_sum / num_rounds)
    standard_deviation = variance ** 0.5
    print("/* === CV average precision %.3f%% (+-SD %.3f%%) */" % (correct / (num_rounds * total) * 100, standard_deviation / total * 100))


if __name__ == "__main__":
    concepts_filename = sys.argv[1]
    gs_filename = sys.argv[2]
    gold_standard = json.load(open(gs_filename))
    input_list = json.load(open(concepts_filename))
    concepts = load(input_list, gold_standard)

    print()
    print("/* %d-fold cross-validation (with %.2f test splits): */" % (num_rounds, test_portion))
    cross_validate_all(concepts)

    print()
    classifier = train_model(concepts)
    test_model('Training set', concepts, classifier)
    dump_model(classifier)
