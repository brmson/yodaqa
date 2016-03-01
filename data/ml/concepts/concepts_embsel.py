#!/usr/bin/python3 -u
#
# concepts_embsel - generate dataset for training embedding-based question/description classifier
#
# Example:
# mkdir data/ml/embsel/concepts-moviesF-train
# data/ml/concepts/concepts_embsel.py data/ml/concepts/questionDump.json ../dataset-factoid-movies/moviesF/entity-linking.json data/ml/embsel/concepts-moviesF-train
#
# This uses the https://github.com/brmson/Sentence-selection infrastructure
# for training the classifier.

from __future__ import print_function
from __future__ import division

import json
import re
import sys

sys.path.append('../dataset-factoid-webquestions/scripts')
import datalib


def load(qdump, gs):
    data = datalib.QuestionSet()
    data.add(qdump)
    for g in gs:
        g['gsConcept'] = g.pop('Concept')  # rename conflicting key name
    data.add(gs)
    return data


def qrepr(q):
    """ return list of tokens representing the question; XXX: redundant to PropertyGloVeScoring.questionRepr() """
    return [l['text'] for l in q['LAT'] if l['type'] != 'WordnetLAT'] + q['SV']


def crepr(c):
    """ return lits of tokens representing the concept; PropertyGloVeScoring.tokenize(description) """
    firstCrisp = c.get('description', '')
    return re.findall(r"\w+|[^\w\s]", firstCrisp.lower(), re.UNICODE)


def jacana_dump(q, f):
    """ dump q concepts in jacana format (suitable for Sentence-selection) """
    gsPIDs = [c['pageID'] for c in q['gsConcept']]
    print('<Q> ' + ' '.join(qrepr(q)), file=f)
    for c in q['Concept']:
        isCorrect = 1 if c['pageID'] in gsPIDs else 0
        print('%d 1 %s' % (isCorrect, ' '.join(crepr(c))), file=f)


if __name__ == "__main__":
    with open(sys.argv[1], 'r') as f:
        qdump = json.load(f)
    with open(sys.argv[2], 'r') as f:
        gs = json.load(f)
    outdir = sys.argv[3]

    data = load(qdump, gs)

    for q in data.to_list():
        with open('%s/%s-prop.txt' % (outdir, q['qId']), 'w') as f:
            jacana_dump(q, f)
