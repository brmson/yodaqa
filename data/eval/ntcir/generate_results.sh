#!/bin/bash
question_dir=$1
answer_sheet_dir=$2

for dir in ${question_dir}/*; do
	if [ -d "$dir" ]; then
	  	for f in ${dir}/*; do
			filename="${f##*/}"
			filename="${filename%.*}"
			echo $filename
			python data/eval/ntcir/make-tsv.py $f > tmp.tsv
			./gradlew tsvgs -PexecArgs="tmp.tsv output-${filename}.tsv" 2>&1 | tee data/eval/ntcir/log
			rm -f tmp.tsv
			python data/eval/ntcir/answers-to-xml.py output-${filename}.tsv ${answer_sheet_dir}/${filename}.xml > ${filename}-answer-sheet.xml
			python data/eval/ntcir/log-to-xml.py data/eval/ntcir/log data/eval/ntcir/${filename}-results.xml
			rm -f data/eval/ntcir/log		
		done
	fi
	
done
