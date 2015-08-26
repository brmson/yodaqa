#!/usr/bin/python -u
#
# Train a logistic regression classifier for question type classification
# 
# usage: train_question_classifier.py [-h]
#                                     train_data train_data_tsv test_data
#                                     test_data_tsv
# 
# The tsv files need to have following structure: exactly 4 columns separated by tabs.
# First column is question ID, second could be anything because it is not relevat for this classification,
# but important for feature dump input structure, third column is the actual question and fourth is the question label. 
# 
# The json files (train_data and test_data parameters) are simply generated fron tsv files described above.
# The dumping of features is done by running questionDump from yodaqa. In the yodaqa root, you need to run this command:
# 	./gradlew questionDump -PexecArgs="data-in.tsv data-out.json"
# This command needs to be run twice (once for train-data and once for test-data).
# 
# The only reason you need to pass tsv files to this script too is that question-dump does not dump actual question 
# but only the features. 
# 
# The script prints model in json format to standard output.


from argparse import ArgumentParser
from sklearn.feature_extraction import DictVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn import cross_validation
import json
import sys

def q_to_fdict(q):
    fdict = {}
    for lat in q['LAT']:
        if (lat['type'] != "WordnetLAT"):
            fdict['lat/' + lat['text'] + '/' + lat['type']] = 1
    for sv in q['SV']:
        fdict['sv'] = sv
    if (len(q['SV']) == 0):
        fdict['sv_not_present'] = 1
    return fdict

if __name__ == '__main__':
	parser = ArgumentParser(description='Training question classifier')	
	parser.add_argument("train_data", help="training data set in json format with features")
	parser.add_argument("train_data_tsv", help="training data set in tsv format with labels")
	parser.add_argument("test_data", help="testing data set in json format with features")
	parser.add_argument("test_data_tsv", help="testing data set in tsv format with labels")
	args = parser.parse_args() 

	with open(args.train_data, 'r') as f:
	    fdict = [q_to_fdict(q) for q in json.load(f)]
	    Xdict = DictVectorizer()
	    trainX = Xdict.fit_transform(fdict)
    
	with open(args.test_data, 'r') as f:
	    fdict = [q_to_fdict(q) for q in json.load(f)]
	    testX = Xdict.transform(fdict)

	with open(args.train_data_tsv, 'r') as f:
		trainY = [line.split("\t")[3].replace("\n","") for line in f]

	with open(args.test_data_tsv, 'r') as f:
		testY = [line.split("\t")[3].replace("\n","") for line in f]

	cfier = LogisticRegression(solver='lbfgs', multi_class='multinomial')
	cfier.fit(trainX, trainY)
	print ("// Accuracy on training set: " + str(cfier.score(trainX, trainY)))

	#Temporary solution: cross validation
	res = cross_validation.cross_val_score(cfier, trainX, trainY, cv=10)
	print ("// Average accuracy over 10-fold cross valiadtion: " + str(sum(res) / float(len(res))))
	print ("// Accuracy on test data set: " + str(cfier.score(testX, testY)))
	print ("// Logistic Regression parameters: " + str(cfier.get_params()))
	data = {}
	data["weight_vector"] = cfier.coef_.tolist()
	data["intercept"] = cfier.intercept_.tolist()
	data["feature_indices"] = Xdict.vocabulary_
	lab = set()
	[lab.add(e) for e in trainY]
	data["labels"] = sorted(lab)
	json.dump(data, sys.stdout)
