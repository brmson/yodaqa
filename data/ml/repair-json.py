#!/usr/bin/python
#
# Repairs questionDump json output to have a correct syntax.
#
# Adding "[" to the beginning, "]" to the end and "," between lines.
#
# Usage: repair-json.py [-h] input_data

from argparse import ArgumentParser

if __name__ == '__main__':
	parser = ArgumentParser(description='Add brackets and commas to json')	
	parser.add_argument("input_data", help="input json file")
	args = parser.parse_args() 

	print ("[")
	with open(args.input_data, 'r') as f:
		lines = f.readlines()
		for i, line in enumerate(lines):
			if i != len(lines) - 1:
				print (line.replace("\n", "") + ",")
			else:
				print (line.replace("\n", ""))
	print ("]")
