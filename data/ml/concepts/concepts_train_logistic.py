#!/usr/bin/pypy
from __future__ import print_function
from __future__ import division
import numpy as np
import sys
import json
import random
from sklearn import linear_model

#cross validation parameters
num_rounds = 10
test_portion = 1.0 / 5


def label(input_list, gold_standard):
    concept = []
    labels = []
    correct_counter = 0
    incorrect_counter = 0
    for q, q_gs in zip(input_list, gold_standard):
        for conc in q['Concept']:
            valid = False
            for corr in q_gs['Concept']:
                if conc['fullLabel'] == corr['fullLabel']:
                    valid = True
                    break
            if valid is True:
                labels.append(1)
                correct_counter += 1
            else:
                labels.append(0)
                incorrect_counter += 1
            concept.append(
                (float(conc['editDist']),
                 float(conc['probability']),
                 float(conc['score']),
                 float(conc['getByLAT']),
                 float(conc['getByNE']),
                 float(conc['getBySubject']),
                 float(conc['getByFuzzyLookup']),
                 float(conc['getByCWLookup'])))
    print("correct: " + str(correct_counter))
    print("incorrect: " + str(incorrect_counter))
    pr = correct_counter / incorrect_counter * 100
    print("correct percent: " + str(pr))
    return (concept, labels)


def main():
    concepts_filename = sys.argv[1]
    gs_filename = sys.argv[2]
    gold_standard = json.load(open(gs_filename))
    input_list = json.load(open(concepts_filename))
    ll = label(input_list, gold_standard)
    features = np.asarray(ll[0])
    labels = np.asarray(ll[1])
    classifier = train(features, labels)
    print("coeficients: " + str(classifier.coef_))
    print("intercept: " + str(classifier.intercept_))
    print("starting cross validation")
    cross_validate_all(ll)


def train(features, labels):
    clf = linear_model.LogisticRegression(C=1, penalty='l2', tol=1e-5)
    clf.fit(features, labels)
    return clf


def split_dataset(ll):
    """
    Create a random split of (fv, labels) tuple list to training and test set
    """
    numrows = len(ll[0])
    newlist = list(zip(ll[0], ll[1]))
    random.shuffle(newlist)
    teststart = int((1.0 - test_portion) * numrows)
    train_list = newlist[0:teststart]
    test_list = newlist[teststart:]
    fv_train = np.asarray(map(lambda x: x[0], train_list))
    label_train = np.asarray(map(lambda x: x[1], train_list))
    fv_test = np.asarray(map(lambda x: x[0], test_list))
    label_test = np.asarray(map(lambda x: x[1], test_list))
    return (fv_train, label_train, fv_test, label_test)


def cross_validate(ll):
    (fv_train, label_train, fv_test, label_test) = split_dataset(ll)
    classifier = train(fv_train, label_train)
    return test_model(fv_test, label_test, classifier)


def cross_validate_all(ll):
    random.seed(1234567)
    total = 0
    correct = 0
    mean = 0
    res = []
    for _ in range(num_rounds):
        (c, i, t) = cross_validate(ll)
        res.append((c, i, t))
        correct += c
    total = t
    mean = correct / num_rounds
    x_sum = 0
    for triplet in res:
        x_sum += (triplet[0] - mean) ** 2
    variance = (x_sum / num_rounds)
    standard_deviation = variance ** 0.5
    print("---")
    print("average: " + str(correct / (num_rounds * total) * 100))
    print("standard_deviation: " + str(standard_deviation / total * 100))


def test_model(fv_test, label_test, cfier):
    proba = cfier.predict_proba(fv_test)
    corr = 0
    incorr = 0
    prediction = 0
    total = len(fv_test)
    for entry, correct in zip(proba, label_test):
        if entry[0] > 0.5:
            prediction = 0
        else:
            prediction = 1
        if prediction == correct:
            corr += 1
        else:
            incorr += 1
    stat = corr / total * 100
    print ("%d out of %d , precision  %f" % (corr, total, stat))
    return (corr, incorr, total)

if __name__ == "__main__":
    main()
