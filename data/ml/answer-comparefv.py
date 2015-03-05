#!/usr/bin/python -u
#
# Compare feature vectors of two different question/answers sets
#
# Usage: answer-comparefv.py QUESTIONS.tsv CSVDIR1 CSVDIR2
#
# Note that this will cover *only* answers common to both sets,
# and not list any new/gone answers.
#
# Example: data/ml/answer-comparefv.py data/eval/curated-train.tsv data/eval/answer-csv/52e712d data/eval/answer-csv/13a9870

import sys
import answerfv


def compare_fv(fields, a1, a2):
    delta = []
    for f in fields:
        if f == '' or f == 'confidence' or f == '@simpleScore':
            continue
        f1 = a1.get(f, 0)
        f2 = a2.get(f, 0)
        if f1 == f2:
            continue
        delta.append((f, str(f1), str(f2)))
    return delta


def delta2str(delta):
    strdelta = []
    for f, f1, f2 in delta:
        f = f.replace('@', '')
        if f1 in ['0.0', '1.0', '-1.0'] and f2 in ['0.0', '1.0', '-1.0']:
            if f2 != '0.0':
                strdelta.append('+%s' % (f,))
            else:
                strdelta.append('-%s' % (f,))
        elif ';' in f1 or ';' in f2:
            f1 = f1.split(';')
            f2 = f2.split(';')
            minus = ['-%s' % (e,) for e in f1 if e != '' and e not in f2]
            plus = ['+%s' % (e,) for e in f2 if e != '' and e not in f1]
            if minus or plus:
                strdelta.append('%s(%s%s%s)' % (f, ','.join(minus), (' | ' if (plus and minus) else ''), ','.join(plus)))
        else:
            strdelta.append('%s(%s->%s)' % (f, f1[:4], f2[:4]))
    return ', '.join(strdelta)


if __name__ == "__main__":
    qlist, questions = answerfv.load_questions(sys.argv[1])
    for qid in qlist:
        print("[%s] %s  (%s)" % (questions[qid]['id'], questions[qid]['text'], questions[qid]['anspattern']))
        alist1, ans1 = answerfv.load_answers(sys.argv[2], qid)
        alist2, ans2 = answerfv.load_answers(sys.argv[3], qid)
        # List answers in the order of confidence in set #2
        for a in alist2:
            if a not in ans1:
                continue  # Not common answer
            a1 = ans1[a]
            a2 = ans2[a]
            delta = compare_fv(ans2['_header'], a1, a2)
            if delta == []:
                continue
            print("\t%-38.38s\t%s %.3f\t%s" % (a, a2['iM'], float(a2['confidence']), delta2str(delta)))
        print('')
