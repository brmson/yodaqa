# Python module for loading question lists and feature vectors

import csv


def load_questions(tsvfile):
    qlist = list()
    questions = dict()
    tsv = open(tsvfile, mode='r')
    for line in tsv:
        qid, qtype, text, anspattern = line.rstrip().split("\t")
        qid = int(qid)
        questions[qid] = {'id': qid, 'type': qtype, 'text': text, 'anspattern': anspattern}
        qlist.append(qid)
    return qlist, questions


def load_answers(csvdir, qid):
    alist = list()
    answers = dict()
    csvfile = '%s/%d.csv' % (csvdir, qid)
    with open(csvfile, mode='r') as csvf:
        reader = csv.DictReader(csvf)
        for answer in reader:
            alist.append(answer['answer'])
            answers[answer['answer']] = answer
        answers['_header'] = reader.fieldnames
    return alist, answers
