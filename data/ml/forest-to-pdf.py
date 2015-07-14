#!/usr/bin/python
#
# Creates pdf file with decision trees.
#
# usage: forest-pdf.py [-h] input_file output_file
# positional arguments:
#   input_file   input pkl file
#   output_file  output pdf file
# optional arguments:
#   -h, --help   show this help message and exit
#
# This script creates visualisation of decision trees
# and save it into a pdf file.


import joblib
from export_graphviz import export_graphviz
from sklearn.externals.six import StringIO
import pydot
from argparse import ArgumentParser

parser = ArgumentParser()
parser.add_argument("input_file", help="input pkl file")
parser.add_argument("output_file", help="output pdf file")
args = parser.parse_args()

output_pdf_filename =  args.output_file
cfier, labels = joblib.load(args.input_file)
trees_dot_data = StringIO()
trees = [t[0] for i, t in enumerate(cfier.estimators_)]

export_graphviz(trees, out_file=trees_dot_data, feature_names=labels)
pydot.graph_from_dot_data(trees_dot_data.getvalue()).write_pdf(output_pdf_filename)
