__author__ = 'honza'
from sklearn.externals import joblib
import json
from argparse import ArgumentParser

parser = ArgumentParser()
parser.add_argument("model_file", help="pkl file with model")
args = parser.parse_args()

cfier, labels = joblib.load(args.model_file)

data = {}
forest = []
for tree in cfier.estimators_:
    line = dict()
    line["children_left"] = tree[0].tree_.children_left.tolist()
    line["children_right"] = tree[0].tree_.children_right.tolist()
    line["features"] = tree[0].tree_.feature.tolist()
    line["thresholds"] = tree[0].tree_.threshold.tolist()
    line["values"] = [x[0][0] for x in tree[0].tree_.value]
    forest.append(line)

data["prior"] = cfier.init_.prior
data["learning_rate"] = cfier.learning_rate
data["forest"] = forest
with open("model.json", "w") as f:
     json.dump(data, f)