#!/bin/bash
if [ $1 = "sh" ] 
	then
	echo "Running shell version..."
	docker run -it --entrypoint="./gradlew" yodaqa run -q 2>&1 | tee /home/logs/yodaqa-it-`date +%s`.log
elif [ $1 = "bing" ]
	then
        echo "Running web version with Bing..."
        docker run -it -p 4567:4567 --entrypoint="./gradlew" yodaqa web -q -Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug-Dcz.brmlab.yodaqa.use_bing=yes 2>&1 | tee /home/logs/yodaqa-web-`date +%s`.log
	else
	echo "Running web version..."
	docker run -it -p 4567:4567 --entrypoint="./gradlew" yodaqa web -q 2>&1 | tee /home/logs/yodaqa-web-`date +%s`.log
fi
