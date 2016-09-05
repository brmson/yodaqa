#!/bin/bash
# Removes all exited containers and images whose build failed
docker rm $(docker ps -a -q -f status=exited)
docker rmi $(docker images | grep "^<none>" | awk "{print $3}")
