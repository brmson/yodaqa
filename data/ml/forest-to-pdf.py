#!/usr/bin/python
#
# Creates pdf file with decision trees.
#
# usage: forest-pdf.py [-h] input_file output_dir
# positional arguments:
#   input_file   input pkl file
#   output_dir   output pdf directory
# optional arguments:
#   -h, --help   show this help message and exit
#
# This script creates visualisation of decision trees
# and save it into a set of pdf files.


import joblib
from forest_graphviz import export_graphviz
from sklearn.externals.six import StringIO
import pydot
from argparse import ArgumentParser

parser = ArgumentParser()
parser.add_argument("input_file", help="input pkl file")
parser.add_argument("output_dir", help="output pdf directory")
args = parser.parse_args()

cfier, labels = joblib.load(args.input_file)
trees = [t[0] for i, t in enumerate(cfier.estimators_)]

for i, tree in enumerate(cfier.estimators_):
    tree_dot_data = StringIO()
    export_graphviz(tree, out_file=tree_dot_data, feature_names=labels)
    pydot.graph_from_dot_data(tree_dot_data.getvalue()).write_pdf('%s/%03d.pdf' % (args.output_dir, i))
