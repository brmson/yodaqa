#!/bin/bash
question_dir=$1

for dir in ${question_dir}/*; do
	if [ -d "$dir" ]; then
	  	for f in ${dir}/*; do
			filename="${f##*/}"
			filename="${filename%.*}"
			echo $filename
			python data/eval/ntcir/make-tsv.py $f > tmp.tsv
			./gradlew tsvgs -PexecArgs="tmp.tsv /dev/null" 2>&1 | tee data/eval/ntcir/log
			rm -f tmp.tsv
			python data/eval/ntcir/log-to-xml.py data/eval/ntcir/log data/eval/ntcir/${filename}-results.xml
			# rm -f data/eval/ntcir/log		
		done
	fi
	
done