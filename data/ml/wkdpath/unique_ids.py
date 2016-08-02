#!/usr/bin/python

import sys, json

if __name__ == '__main__':
	file_name = sys.argv[1]
	with open(file_name) as f:
		jobj = json.load(f)
	gid = 0
	print('[')
	for i, line in enumerate(jobj):
		line['qId'] = str(gid)
		gid += 1
		if (i + 1 == len(jobj)):
			print(json.dumps(line))
		else:
			print(json.dumps(line) + ',')
	print(']')