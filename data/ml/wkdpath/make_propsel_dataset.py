#!/usr/bin/python3
#
# Generates dataset for training neural network based classifier implemented in dataset-sts.
#
# Usage: ./make_propsel_dataset.py SPLIT DATASET_DIR OUTPUT_DIR
# Example: ./make_propsel_dataset.py train questions/ propsel/
#
# The dataset dir needs to contain d-dump, d-property-dump and main folders
# with YodaQA question dump, property dump (using generate_properties.py) and 
# original qestion text with answers respectively.


import json
from nltk.tokenize import word_tokenize
import csv
import sys

if __name__ == '__main__':
    PROP_SEP = " # "
    ENT_TOK = "ENT_TOK"

    split = sys.argv[1]
    dataset_dir = sys.argv[2]
    out_dir = sys.argv[3]

    dump_file = dataset_dir + "/d-dump/" + split + ".json"
    all_paths_file = dataset_dir + "/d-property-dump/" + split + ".json"
    main_file = dataset_dir + "/main/" + split + ".json"

    with open(main_file) as f:
        main = json.load(f)
        
    properties_map = {}
    with open(all_paths_file) as f:
        jsobj = json.load(f)
        for line in jsobj:
            properties_map[line['qId']] = line['allRelations']

    dump = {}
    with open(dump_file) as f:
        jsobj = json.load(f)
        for line in jsobj:
            dump[line['qId']] = line

    out = open(out_dir + '/' + split + '.json', 'w')
    outcsv = csv.DictWriter(out, fieldnames=['qtext', 'label', 'atext'])
    outcsv.writeheader()

    for line in main:
        qtext = line['qText'].lower()
        if (qtext[-1] != "?"):
            if (qtext[-1] == ' '):
                qtext = qtext + "?"
            else:
                qtext = qtext + " ?"
        elif (qtext[-2] != ' '):
            qtext = qtext[:-1] + " ?" 
        
        print(qtext)
        answers = set(line['answers'])
        for prop in properties_map[line['qId']]:
            label = 1 if answers == set(prop['values']) else 0
            outcsv.writerow({'qtext': qtext, 'label': label, 'atext': prop['label']})

    out.close()
