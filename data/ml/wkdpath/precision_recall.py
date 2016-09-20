#!/usr/bin/python3

import csv, sys, re

MONTHS = {'01': 'ledna', '02': 'února', '03': 'března', '04': 'dubna', '05': 'května', '06': 'června',
			'07': 'července', '08': 'srpna', '09': 'září', '10': 'října', '11': 'listopadu', '12': 'prosince'}

def modify_date(orig):
	m = re.match(r'(\d{4,4})-(\d{2,2})-(\d{2,2})T00', orig)
	if m:
		day = str(int(m.group(3)))
		year = str(int(m.group(1)))
		return day + '. ' + MONTHS[m.group(2)] + ' ' + year
	else:
		return orig

def normalize_gs_date(orig):
	m = re.match(r'(\d{4,4})-(\d{2,2})-(\d{2,2})', orig)
	m2 = re.match(r'(\d{1,2})\.(\w+) (\d{4,4})', orig)
	m3 = re.match(r'(\d{1,2})\. ?(\d{1,2})\. ?(\d{4,4})', orig)
	if m:
		day = m.group(3) if m.group(3)[0] != '0' else m.group(3)[1]
		return day + '. ' + MONTHS[m.group(2)] + ' ' + m.group(1)
	elif m2:
		return m2.group(1) + '. ' + m2.group(2) + ' ' + m2.group(3)
	elif m3:
		month = MONTHS[m3.group(2) if len(m3.group(2)) == 2 else '0' + m3.group(2)]
		return m3.group(1) + '. ' + month + ' ' + m3.group(3)
	else:
		return orig


if __name__ == '__main__':
	gs_file = sys.argv[1]
	tsv_file = sys.argv[2]
	answers = {}
	with open(gs_file) as f:
		reader = csv.reader(f, delimiter='\t', escapechar='\\')
		for line in reader:
			answers[line[0]] = line[3].split('|')

	correct = 0
	cnt = 0
	acnt = 0
	precany = 0
	precall = 0
	precg = 0
	recany = 0
	recall = 0
	gcnt = 0
	with open(tsv_file) as f:
		reader = csv.reader(f, delimiter='\t', escapechar='\\')
		for line in reader:
			gcnt += 1
			id = line[0]
			amax = None
			vmax = 0
			for a in line[12:]:
				if len(a.split(':')[0]) == 0:
					continue
				text = a.split(':')[0]
				score = float(a.split(':')[-1])
				if score > vmax:
					vmax = score
					amax = text
			# if amax == 'Nevím':
			# 	continue
			ans = [modify_date(a.split(':')[0]) for a in line[12:] if len(a.split(':')[0]) > 0 and float(a.split(':')[-1]) > 0.0 and a.split(':')[0] != 'Nevím']
			gs_ans = set([normalize_gs_date(re.sub(r'([^\(\)]*)\([^\(\)]*\)', r'\1', a).strip()) for a in answers[id]])
			if (set(ans) == gs_ans):
				correct += 1
			# else:
			# 	print('INCORRECT ', id, line[2], ans, gs_ans)
			if (len(ans) > 0):
				cnt += 1
				precall += 1
			for a in set(ans):
				if (a in gs_ans):
					precany += 1
					break
			for a in set(ans):
				if (a not in gs_ans):
					precall -= 1
					print(id, line[2], ans, gs_ans)
					break
			for a in set(ans):
				acnt += 1
				if (a in gs_ans):
					precg += 1

					
			recall += 1
			for a in gs_ans:
				if (a not in ans):
					recall -= 1
					break
			for a in gs_ans:
				if (a in ans):
					recany += 1
					break


	# print(correct, cnt, precany, recall, recany, precany / cnt, precall, precall / cnt)
	# print(acnt, precg, precg / acnt)
	# print(recall / cnt, recany / cnt)
	# print(recall / gcnt, recany / gcnt)
	# print(gcnt)
	print('Total:', gcnt)
	print('Answered:', cnt)
	print('Precision:', precall / cnt)
	print('Recall:', recall / gcnt)
