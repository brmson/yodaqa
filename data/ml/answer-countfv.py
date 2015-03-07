#!/usr/bin/python -u
#
# Produce feature occurence statistics of a given question/answers set
#
# Usage: answer-countfv.py QUESTIONS.tsv CSVDIR
#
# N.B. this assumes that a feature is set *iff* it has non-zero value
# (but that should generally hold), and it is suitable only for binary
# features overally! (I.e. @originPsgFirst, @noTyCor is suitable,
# @resultLogScore or @spWordNet is not.)
#
# Also, questions with no correct answers are mostly ignored in
# the statistics, they are ignored during training too.
#
# Example: data/ml/answer-countfv.py data/eval/curated-train.tsv data/eval/answer-csv/83aae01
#
# Lists:
# * % of questions containing this feature
# * % of answers (over all questions) containing this feature
#   (very low == liable to overfit)
# * average % of answers per question containing this feature
# * average % of correct answers per question containing this feature
#   (if BOTH of these two are very low == liable to overfit; these two
#   include only questions with the feature generated ever at all)
# * average % of correct answers per question out of all answers that
#   contain the feature, compared to total % of correct answers per
#   question (how good a predictor the feature *individually* is,
#   1.000 being as good as random - the further from 1.000, the stronger
#   negative or positive signal the feature carries)
# * suggestions on whether the feature occurs too rarely, extremely
#   rarely or is a bad isolated predictor; we use reasonable-looking
#   thresholds to check that and if two flags are set, this feature
#   should be blacklisted according to our feature selection method

from __future__ import print_function
import sys
from collections import defaultdict
import answerfv


class AnswerCounter:
    """
    A counter structure that records a number of feature occurences
    in correct and all answers (of a single question).
    """
    def __init__(self):
        self.total_all = 0
        self.total_correct = 0
        self.columns = []
        # Indexed by column names
        self.all_occurs = defaultdict(float)
        self.correct_occurs = defaultdict(float)

    def from_alist(self, qid, alis, ans):
        """
        Populate by data returned by answerfv.load_answers();
        XXX should be static method, call only on fresh instance.
        """
        # Get the list of features
        self.columns = [field for field in ans['_header'] if field.startswith('@')]
        # Process the list of answers
        for a in alist:
            arec = ans[a]

            # Add the features of the answer to the stats dict
            for field in self.columns:
                # A hack for old CSV files that might have out of place quoting
                try:
                    float(arec[field])
                except (ValueError, TypeError):
                    print('Warning: q#%d answer <<%s>> has invalid field %s (%s)' % (qid, a, field, arec[field]), file=sys.stderr)
                    continue

                if float(arec[field]) == 0:
                    continue

                if arec['iM'] == '+':
                    self.correct_occurs[field] += 1
                self.all_occurs[field] += 1

            if arec['iM'] == '+':
                self.total_correct += 1
            self.total_all += 1

    def portion_all_occurs(self, field):
        """
        Return % of all answers that carry the given feature.
        """
        if self.total_all > 0:
            return float(self.all_occurs[field]) / self.total_all
        else:
            return 0

    def portion_correct_occurs(self, field):
        """
        Return % of correct answers that carry the given feature.
        """
        if self.total_correct > 0:
            return float(self.correct_occurs[field]) / self.total_correct
        else:
            return 0

    def portion_occurs_in_correct(self, field):
        """
        Return % of answers carrying the given feature that are correct.
        """
        if self.all_occurs[field] > 0:
            occurs_in_correct = float(self.correct_occurs[field]) / self.all_occurs[field]
            correct_portion = float(self.total_correct) / self.total_all
            return occurs_in_correct / correct_portion
        else:
            return 0


class QACounter:
    """
    Collection of AnswerCounters for many questions, maintaining some aggregate
    statistics.
    """
    def __init__(self, columns=None):
        self.acounters = []
        # columns list can be lazy-initialized on first add_counter()
        self.columns = columns
        self.total_all_ans = 0
        self.total_correct_ans = 0

        # Indexed by column names, lists of per-question values:
        # absolute values
        self.all_occurs = defaultdict(list)
        self.correct_occurs = defaultdict(list)
        self.all_occurs_sum = defaultdict(int)  # cached sum
        self.correct_occurs_sum = defaultdict(int)  # cached sum
        self.any_occurs = defaultdict(int)  # cached sum of non-zero all_occurs
        # per-question percentages; note that we count these only for
        # questions where the answer feature occurs at all
        self.all_occurs_pqp = defaultdict(list)
        self.correct_occurs_pqp = defaultdict(list)
        self.occurs_correct_pqp = defaultdict(list)

    def add_question(self, acounter):
        if self.columns is None:
            self.columns = acounter.columns
        if acounter.total_correct == 0:
            # Sorry, we ignore any questions with no correct answers
            return

        self.acounters.append(acounter)

        self.total_all_ans += acounter.total_all
        self.total_correct_ans += acounter.total_correct

        for field in self.columns:
            self.all_occurs[field].append(acounter.all_occurs[field])
            self.correct_occurs[field].append(acounter.correct_occurs[field])
            self.all_occurs_sum[field] += acounter.all_occurs[field]
            self.correct_occurs_sum[field] += acounter.correct_occurs[field]

            if acounter.all_occurs[field] > 0:
                self.all_occurs_pqp[field].append(acounter.portion_all_occurs(field))
                self.correct_occurs_pqp[field].append(acounter.portion_correct_occurs(field))
                self.occurs_correct_pqp[field].append(acounter.portion_occurs_in_correct(field))
                self.any_occurs[field] += 1

    def portion_questions(self, field):
        """
        Return % of questions that have some answer carrying the feature.
        """
        return float(self.any_occurs[field]) / len(self.acounters)

    def portion_answers(self, field):
        """
        Return % of answers (over all questions) that carry the feature.

        (I.e. essentially mean_portion_all_answers() with the mean
        weighed by total number of answers per each question, plus including
        even questions with this feature not generated at all.)
        """
        return float(self.all_occurs_sum[field]) / self.total_all_ans

    def mean_portion_all_answers(self, field):
        """
        Return average % of answers per question that carry the feature.

        This counts only questions where the feature has been generated
        for some answer.
        """
        if self.any_occurs[field] > 0:
            return float(sum(self.all_occurs_pqp[field])) / self.any_occurs[field]
        else:
            return 0

    def mean_portion_correct_answers(self, field):
        """
        Return average % of correct answers per question that carry the feature.

        This counts only questions where the feature has been generated
        for some answer.
        """
        if self.any_occurs[field] > 0:
            return float(sum(self.correct_occurs_pqp[field])) / self.any_occurs[field]
        else:
            return 0

    def mean_portion_answers_correct(self, field):
        """
        Return average % of answers per question carrying the feature that
        are correct.

        This counts only questions where the feature has been generated
        for some answer.
        """
        if self.any_occurs[field] > 0:
            return float(sum(self.occurs_correct_pqp[field])) / self.any_occurs[field]
        else:
            return 0

    def policy_too_rare(self, field):
        """
        Policy decision on whether the feature occurs too rarely.
        """
        return qacounter.portion_answers(field) < 0.01

    def policy_extremely_rare(self, field):
        """
        Policy decision on whether the feature occurs extremely rarely.
        """
        return qacounter.portion_answers(field) < 0.001 and qacounter.portion_questions(field) < 0.01

    def policy_bad_predictor(self, field):
        """
        Policy decision on whether the feature is a bad predictor
        when used in isolation.
        """
        return abs(qacounter.mean_portion_answers_correct(field) - 1.0) < 0.1


def stats_report(qacounter):
    # ``all_occurs`` is a dict from feature name to a list of per-question
    # fields, each being a count of answers in this question
    print("%38.38s" % ('',) + "\tin %Q\tin %A\tavg%A\tavg%CA\tavg%CAp\tRR B")
    for field in qacounter.columns:
        print("%38.38s\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%c%c %c" %
              (field,
               qacounter.portion_questions(field),
               qacounter.portion_answers(field),
               qacounter.mean_portion_all_answers(field),
               qacounter.mean_portion_correct_answers(field),
               qacounter.mean_portion_answers_correct(field),
               '!' if qacounter.policy_too_rare(field) else '.',
               '!' if qacounter.policy_extremely_rare(field) else '.',
               '!' if qacounter.policy_bad_predictor(field) else '.',
               ))


if __name__ == "__main__":
    """ Gather data """

    qlist, questions = answerfv.load_questions(sys.argv[1])

    qacounter = QACounter()

    for qid in qlist:
        alist, ans = answerfv.load_answers(sys.argv[2], qid)

        acounter = AnswerCounter()
        acounter.from_alist(qid, alist, ans)
        qacounter.add_question(acounter)

    """ Report data """
    stats_report(qacounter)
