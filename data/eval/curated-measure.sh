#!/bin/sh
#
# Usage: curated-measure.sh {test,train}

type="$1"
args=
case $type in
	test) ;;
	train) args=-Dcz.brmlab.yodaqa.mltraining=1;;
	*) echo "Usage: curated-measure.sh {test,train}" >&2; exit 1;;
esac

outfile="data/eval/tsv/curated-${type}-ovt-$(git rev-parse --short HEAD).tsv"
mkdir -p data/eval/tsv

MAVEN_OPTS="-XX:-UseParallelGC -XX:-UseConcMarkSweepGC" \
	time mvn verify exec:java -Ptsvgs \
	-Dexec.args="data/eval/curated-${type}.tsv $outfile" \
	-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa.analysis=debug \
	$args 2>&1 | tee logs/curated-${type}-$(git rev-parse --short HEAD).log
echo $outfile
