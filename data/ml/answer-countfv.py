#!/usr/bin/python -u
#
# Produce feature occurence statistics of a given question/answers set
#
# Usage: answer-countfv.py QUESTIONS.tsv CSVDIR
#
# Note that this will cover *only* answers common to both sets,
# and not list any new/gone answers.
#
# Example: data/ml/answer-countfv.py data/eval/curated-train.tsv data/eval/answer-csv/83aae01

from __future__ import print_function
import sys
from collections import defaultdict
import answerfv


def stats_report(columns, all_occurs, correct_occurs):
    print("%38.38s" % ('',) + "\tin %Q\tin avg%A\tin avg%CA")
    for field in columns:
        all_occurs_nz = filter(lambda p: p != 0, all_occurs[field])
        correct_occurs_nz = filter(lambda p: p != 0, correct_occurs[field])
        portion_q = float(len(all_occurs_nz)) / len(all_occurs[field])
        mean_a = float(sum(all_occurs_nz)) / len(all_occurs_nz) if len(all_occurs_nz) > 0 else 0
        mean_ca = float(sum(correct_occurs_nz)) / len(correct_occurs_nz) if len(correct_occurs_nz) > 0 else 0
        print("%38.38s\t%.3f\t%.3f\t%.3f" % (field, portion_q, mean_a, mean_ca))


if __name__ == "__main__":
    """ Gather data """

    qlist, questions = answerfv.load_questions(sys.argv[1])

    # All, correct occurences
    all_occurs = defaultdict(list)
    correct_occurs = defaultdict(list)

    for qid in qlist:
        alist, ans = answerfv.load_answers(sys.argv[2], qid)

        # All, correct occurences within a single question
        all_occurs_q = defaultdict(float)
        correct_occurs_q = defaultdict(float)

        # Get the list of features
        columns = [field for field in ans['_header'] if field.startswith('@')]
        # Process the list of answers
        for a in alist:
            arec = ans[a]

            # Add the features of the answer to the stats dict
            for field in columns:
                # A hack for old CSV files that might have out of place quoting
                try:
                    float(arec[field])
                except (ValueError, TypeError):
                    print('Warning: q#%d answer <<%s>> has invalid field %s (%s)' % (qid, a, field, arec[field]), file=sys.stderr)
                    continue

                if float(arec[field]) == 0:
                    continue

                if arec['iM'] == '+':
                    correct_occurs_q[field] += 1
                all_occurs_q[field] += 1

            if arec['iM'] == '+':
                correct_occurs_q['_total'] += 1
            all_occurs_q['_total'] += 1

        # Store per-question occurence statistics
        for field in columns:
            correct_occurs[field] += [float(correct_occurs_q.get(field, 0)) / correct_occurs_q['_total'] if correct_occurs_q['_total'] > 0 else 0]
            all_occurs[field] += [float(all_occurs_q.get(field, 0)) / all_occurs_q['_total']]

    """ Report data """
    stats_report(columns, all_occurs, correct_occurs)
