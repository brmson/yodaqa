#!/bin/bash
username=$(whoami)
docker run -it -v /home/$username/docker/data/enwiki/collection1/:/solr/example/enwiki/collection1/ --entrypoint="java" -p 8983:8983 enwiki -Dsolr.solr.home=enwiki -jar start.jar

