Scripts
=======
This directory contains scripts for running Docker containers.

## Applications
 - Alquist dialogue manager
 - DBpedia
 - Wikipedia
 - Hub
 - Syntaxnet - Czech parser
 - YodaQA
 - Javadoc for YodaQA

## Scripts
- ``daemon-start.sh`` - starts Docker daemon; should be run as first thing
- ``docker-compose.sh`` - runs YodaQA with all of its backends; needs all images to be built and all data to be in
``/home/fp/docker/data/``
- ``docker-rm.sh`` - Removes all exited containers and all images with <none> tags (means they weren't finished)