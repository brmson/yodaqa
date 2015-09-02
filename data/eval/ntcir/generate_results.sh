#!/bin/bash
question_dir=$1
answer_sheet_dir=$2

mkdir -p data/eval/ntcir/output

for dir in ${question_dir}/*; do
	if [ -d "$dir" ]; then
		echo ${dir##*/}
	  	for f in ${dir}/*; do
			filename="${f##*/}"
			filename="${filename%.*}"
			echo $filename
			python data/eval/ntcir/make-tsv.py $f > tmp.tsv
			./gradlew tsvgs -PexecArgs="tmp.tsv data/eval/ntcir/output/output-${filename}.tsv" 2>&1 | tee data/eval/ntcir/output/log
			rm -f tmp.tsv
			python data/eval/ntcir/answers-to-xml.py data/eval/ntcir/output/output-${filename}.tsv ${answer_sheet_dir}/${dir##*/}/${filename}.xml > data/eval/ntcir/output/${filename}-answer-sheet.xml
			python data/eval/ntcir/log-to-xml.py data/eval/ntcir/output/log data/eval/ntcir/output/${filename}-results.xml
			rm -f data/eval/ntcir/output/log		
		done
	fi
	
done
