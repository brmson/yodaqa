#!/bin/bash
docker run -it -v /home/fp/docker/data/enwiki/collection1/:/solr/example/enwiki/collection1/ --entrypoint="java" -p 8983:8983 enwiki -Dsolr.solr.home=enwiki -jar start.jar

