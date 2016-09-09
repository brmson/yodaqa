#!/bin/bash
# Move to directory with all the data and .yml file
cd /home/fp/docker/data

# Fixes error message about different versions of server and client 
export COMPOSE_API_VERSION=1.18

# Launches all containers
docker-compose up
