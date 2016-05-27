#!/usr/bin/python3 -u
#
# fbpath_emb - generate dataset for training embedding-based freebase path classifier
#
# Usage: fbpath_emb.py RELATION_NUM DUMP.JSON ALLRELATIONS.JSON RELATIONS_GS.JSON OUTDIR
#
# Example:
# mkdir data/ml/fbpath-emb/props-webquestions-train
# data/ml/fbpath-emb/fbpath-emb.py ../dataset-factoid-webquestions/d-dump/trainmodel.json data/ml/fbpath-emb/relations/trainmodel.json ../dataset-factoid-webquestions/d-freebase-brp/trainmodel.json data/ml/fbpath-emb/props-webquestions-train
#
# This uses the https://github.com/brmson/Sentence-selection infrastructure
# for training the classifier.
#
# RELATION_NUM            - 1 for concept relations, 2 for relations of connected concept, 3 for witness relations
# DUMP.JSON               - yodaqa question dump
# RELATIONS_GS.JSON       - gold standard freebase-paths
# OUTDIR                  - output directory

from __future__ import print_function
from __future__ import division

import json
import re
import sys

sys.path.append('../dataset-factoid-webquestions/scripts')
import datalib


def load(qdump, rel, gs):
    data = datalib.QuestionSet()
    data.add(qdump)
    data.add(rel)
    data.add(gs)
    return data


def qrepr(q):
    """ return list of tokens representing the question; XXX: redundant to PropertyGloVeScoring.questionRepr() """
    return [l['text'] for l in q['LAT'] if l['type'] != 'WordnetLAT'] + q['SV']


def rrepr(c):
    """ return lits of tokens representing the concept; PropertyGloVeScoring.tokenize(description) """
    firstCrisp = c.get('label', '')
    return re.findall(r"\w+|[^\w\s]", firstCrisp.lower(), re.UNICODE)


def jacana_dump(q, f, rel_num, gs):
    """ dump q concepts in jacana format (suitable for Sentence-selection) """
    print('<Q> ' + ' '.join(qrepr(q)), file=f)
    for r in q['allRelations']:
        isCorrect = 1 if r['property'] in gs else 0
        print('%d 1 %s' % (isCorrect, ' '.join(rrepr(r))), file=f)


if __name__ == "__main__":
    relation_num = int(sys.argv[1])
    if (relation_num not in [1, 2, 3]):
        exit("Wrong relation number. Must be 1, 2 or 3")
    with open(sys.argv[2], 'r') as f:
        qdump = json.load(f)
    with open(sys.argv[3], 'r') as f:
        rel = json.load(f)
    with open(sys.argv[4], 'r') as f:
        gs = json.load(f)
    outdir = sys.argv[5]

    data = load(qdump, rel, gs)

    for q in data.to_list():
        if (q['relPaths'] == [] or q['allRelations'] == []):
            continue
        gs = [p[0][relation_num - 1][1:].replace("/",".") for p in q['relPaths'] if len(p[0]) >= relation_num]
        has_second_relation = len([1 for p in q['relPaths'] if len(p[0]) >= relation_num - 1]) > 0
        if ((len(gs) == 0 and relation_num != 3) or (relation_num == 3 and not has_second_relation)):
            continue
        with open('%s/%s-prop.txt' % (outdir, q['qId']), 'w') as f:
            jacana_dump(q, f, relation_num, gs)
