#!/bin/sh
# Download and preprocess TREC2000-2003 QA datasets - questions
# and answer patterns.

# 1999 data is very specific to the supplied corpus rather than being generic trivia
# http://trec.nist.gov/data/qa/T8_QAdata/topics.qa_questions.txt http://trec.nist.gov/data/qa/T8_QAdata/adjudicated_for_perl
# 2004 data is grouped into "topics" and that's too weird for us now
# http://trec.nist.gov/data/qa/2004_qadata/QA2004_testset.xml
# http://trec.nist.gov/data/qa/2004_qadata/04.patterns.zip; unzip 04.patterns.zip trec13factpats.txt

datasets="
2000 http://trec.nist.gov/data/qa/T9_QAdata/qa_questions_201-893 http://trec.nist.gov/data/qa/T9_QAdata/patterns
2001 http://trec.nist.gov/data/qa/2001_qadata/main_task_QAdata/qa_main.894-1393.txt http://trec.nist.gov/data/qa/2001_qadata/main_task_QAdata/patterns.trec10
2002 http://trec.nist.gov/data/qa/2002_qadata/main_task_QAdata/t11_500_numbered.txt http://trec.nist.gov/data/qa/2002_qadata/main_task_QAdata/patterns.txt
2003 http://trec.nist.gov/data/qa/2003_qadata/03QA.tasks/test.set.t12.txt http://trec.nist.gov/data/qa/2003_qadata/03QA.tasks/t12.pats.txt
"

export LC_COLLATE=C

echo "$datasets" | while read year urlq urlp; do
	[ -n "$urlp" ] || continue
	echo $year
	curl -s "$urlq" | ./trec2q.pl >"trec$year-q.tsv"
	curl -s "$urlp" | sort -n | ./trec2p.pl >"trec$year-p.tsv"
	join -t '	' trec$year-q.tsv trec$year-p.tsv >trec${year}.tsv
done

cat trec????.tsv >trec-all.tsv # All TREC questions
cat trec????.tsv | grep -v '|' >trec-single.tsv # Questions that have only single expected answer

# trecnew is TREC 11, 12; these datasets seem to be most mature,
# used e.g. in Chu-Carroll, Fan: Leveraging Wikipedia Characteristics...
cat trec2002.tsv trec2003.tsv >trecnew-all.tsv
cat trec2002.tsv trec2003.tsv | grep -v '|' >trecnew-single.tsv
