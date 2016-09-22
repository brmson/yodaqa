#!/bin/bash
username=$(whoami)
docker run -it -v /home/$username/docker/data/db/:/jena-fuseki/db/ --entrypoint="./fuseki-server" -p 3037:3037 fuseki --port 3037 --loc db /dbpedia
