#!/usr/bin/python -u
#
# Makes tsv files in format accepted by questionDump from question classification dataset 
# obtained from here: http://cogcomp.cs.illinois.edu/Data/QA/QC/.
# 
# usage: make-4column-tsv.py [-h] input_data
# 
# Input data set needs to have following structure: "coarse_label:fine_label question"


from __future__ import print_function
from argparse import ArgumentParser

if __name__ == '__main__':
	parser = ArgumentParser(description='Creates 4-column tsv suitable for feature dump')	
	parser.add_argument("input_data", help="Input data set with structure: \"coarse_label:fine_label question\"")
	args = parser.parse_args() 

	with open(args.input_data, 'r') as f:
	    for i, line in enumerate(f):
	    	full_label, question = line.replace("\n","").split(" ", 1)
	    	coarse_label = full_label.split(":")[0]
	    	print (i, full_label, question, coarse_label, sep="\t")