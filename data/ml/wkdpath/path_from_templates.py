#!/usr/bin/python3
#
# Generates gold standard of wikidata queries for given questions according to their templates.
# This script is only applicable to a set of questions generated using templates described in this file.
# These question also need to be saved in the files with a specific name representing given template.
#
# Usage: ./path_from_template.py QUESTION_DIR QUESTION_SUBSET
# QUESTION_DIR 		- contains questions in json format saved in files with names (datumnarozenihuman.json, ...)
# QUESTION_SUBSET 	- a json file containing subset of questions from the directory above (possibly a mix of questions from different files)

import os, sys, json


mapping = {	'datumnarozenihuman.json': [['?res wdt:P569 ?valres']],
			'dcerahuman.json': [['?res wdt:P40 ?valres', '?valres wdt:P21 wd:Q6581072']],
			'detihuman.json': [['?res wdt:P40 ?valres']],
			'humandiskografie.json': [['?valres wdt:P175 ?res']],
			'humanfilmografie.json': [['?valres wdt:P161 ?res']],
			'humanfilmy.json': [['?valres wdt:P161 ?res']],
			'humankniha.json': [['?valres wdt:P50 ?res']],
			'humanknihy.json': [['?valres wdt:P50 ?res']],
			'humanmanzel.json': [['?res wdt:P26 ?valres']],	#'humanmanzel.json': [['wdt:P21', 'wd:Q6581072', 'wdt:P26']],
			'humanmanzelka.json': [['?res wdt:P26 ?valres']],	#'humanmanzelka.json': [['wdt:P21', 'wd:Q6581097', 'wdt:P26']],
			'humanoficialnistranky.json': [['?res wdt:P856 ?valres']],	#'humanoficialnistranky.json': [['wdt:P31', 'wd:Q5', 'wdt:P856']],
			'humanpisnicky.json': [['?valres wdt:P175 ?res', '?valres wdt:P31 wd:Q134556'], ['?valres wdt:P175 ?res', '?valres wdt:P31 wd:Q7302866']],
			'humanpritel.json': [['?res wdt:P451 ?valres']],
			'humanpritelkyne.json': [['?res wdt:P451 ?valres']],
			'humanvyska.json': [['?res wdt:P2048 ?valres']],
			'kdyzemrelhuman.json': [['?res wdt:P570 ?valres']],
			'kdezemrelhuman.json':[['?res wdt:P20 ?valres']],
			'matkahuman.json': [['?res wdt:P25 ?valres']],
			'otechuman.json': [['?res wdt:P22 ?valres']],
			'synhuman.json': [['?res wdt:P40 ?valres', '?valres wdt:P21 wd:Q6581097']],
			'vnukhuman.json': [['?res wdt:P40 ?med', '?med wdt:P40 ?valres', '?valres wdt:P21 wd:Q6581097']]
}

if __name__ == '__main__':
	dir_name = sys.argv[1]
	main_name = sys.argv[2]
	all_map = {}
	for file in os.listdir(dir_name):
		with open(dir_name + '/' + file) as f:
			jobj = json.load(f)
			for line in jobj:
				all_map[line['qId']] = line
				all_map[line['qId']]['relPaths'] = mapping[file]
	print('[')
	with open(main_name) as f:
		jobj = json.load(f)
		for i, line in enumerate(jobj):
			res = {'qId': line['qId'], 'relPaths': all_map[line['qId']]['relPaths']}
			if (i + 1 == len(jobj)):
				print(json.dumps(res))
			else:
				print(json.dumps(res), ',', sep='')

	print(']')			