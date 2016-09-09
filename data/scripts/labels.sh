#!/bin/bash
if [ $1 != "sqlite" ] 
	then
	echo "Running label lookup..."
	docker run -it -v /home/matulma4/label-lookup/:/shared --entrypoint="pypy" -p 5000:5000 labels /label-lookup/lookup-service.py /shared/sorted_list.dat 2>&1 | tee /home/logs/yodaqa-web-`date +%s`.log
	else
	echo "Running sqlite lookup..."
	docker run -it -v /home/matulma4/label-lookup/:/shared --entrypoint="pypy" -p 5001:5001 labels /label-lookup/lookup-service-sqlite.py /shared/labels.db
fi
