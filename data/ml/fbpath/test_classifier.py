#!/usr/bin/python
#
# Usage: fbpath_train_logistic.py TRAIN.JSON VAL.JSON [print]
#
# Trains and validate classifier for branched paths. The optional print parameter tells whether
# to print question text, predicted paths and gold standard for branched paths or not.
# The last line of output contains information about number of branched paths, number of successfully
# predicted paths with probability more than 0.5, number of successfully predicted paths in top 15
# predictions and MRR.

import json
import numpy as np
from fbpathtrain import VectorizedData
import random, time
from sklearn.linear_model import LogisticRegression
from sklearn.multiclass import OneVsRestClassifier
import sys

def check_q(cfier, v, i):
    probs = cfier.predict_proba(v.X.toarray()[i])[0]
    top_probs = sorted(enumerate(probs), key=lambda k: k[1], reverse=True)
    top_lprobs = ['%s: %.3f' % (v.Ydict.classes_[k[0]], k[1]) for k in top_probs[:15]]
    return (sorted(v.Xdict.inverse_transform(v.X[i])[0].keys(), key=lambda s: reversed(s)),
        v.Ydict.inverse_transform(cfier.predict(v.X.toarray()[i]))[0],
        top_lprobs,
        v.Ydict.inverse_transform(np.array([v.Y[i]]))[0])

trainfile = sys.argv[1]
valfile = sys.argv[2]
if (len(sys.argv) > 3 and sys.argv[3] == 'print'):
	print_question = True
else:
	print_question = False


random.seed(17151713)

with open(trainfile, 'r') as f:
    traindata = VectorizedData(json.load(f))
print('// traindata: %d questions, %d features, %d fbpaths' % (
      np.size(traindata.X, axis=0), np.size(traindata.X, axis=1), np.size(traindata.Y, axis=1)))
sys.stdout.flush()

t_start = time.clock()
cfier = OneVsRestClassifier(LogisticRegression(penalty='l1'), n_jobs=4)
cfier.fit(traindata.X, traindata.Y)
t_end = time.clock()
print('// training took %d seconds' % (t_end-t_start,))
sys.stdout.flush()

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

with open(valfile, 'r') as f:
	json_file = json.load(f)
	data =  VectorizedData(json_file, traindata.Xdict, traindata.Ydict)
	predicted = 0
	predicted_in_top = 0
	cnt = 0
	mrr = 0
	for i in range(len(json_file)):
		res = check_q(cfier, data, i)
		for gold in res[3]:
			if (len(gold.split("|")) != 3):
				continue
			cnt += 1
			if (print_question):
				print('qText: %s\nin: %s\nout: %s\noutp: %s\ngold: %s' % (json_file[i]['qText'], res[0], res[1], res[2], res[3]))
			if (gold in res[1]):
				predicted += 1
			outl = [outp.split(":")[0] for outp in res[2]]
			if (gold in outl):
				idx = [i for i, o in enumerate(outl)][0]
				# print(1/(float)(idx+1))
				mrr += 1.0/(idx+1)
				predicted_in_top += 1
	mrr = mrr / cnt
	print("Total branched: %d, predcited: %d, predicted in top 15: %d, MRR: %f" % (cnt, predicted, predicted_in_top, mrr))
