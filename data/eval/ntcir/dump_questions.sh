#!/bin/bash
question_dir=$1

for dir in ${question_dir}/*; do
	if [ -d "$dir" ]; then
	  	for f in ${dir}/*; do
			filename="${f##*/}"
			filename="${filename%.*}"
			echo $filename
			python data/eval/ntcir/make-tsv.py $f > tmp.tsv
			./gradlew questionDump -PexecArgs="tmp.tsv tmp2.json"
			rm -f tmp.tsv
			python data/eval/ntcir/json-to-xml.py tmp2.json data/eval/ntcir/${filename}.xml	data/eval/ntcir/${filename}-typedef.xml
			rm -f tmp2.json		
		done
	fi
	
done