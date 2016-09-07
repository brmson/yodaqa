Backend setup
=============
This directory contains scripts for setting up separate backends. All data is downloaded into ``/home/fp/docker/data``
directory for convenience when running ``docker-compose`.`

## Labels
Run ``label-lookup.sh`` to install both normal and lite versions. Run ``sqlite.sh`` to install only the lite version.
## DBpedia
Run ``dbpedia.sh`` to obtain DBpedia data. Other scripts are called from this script and are not meant to be run separately.
## Wikipedia
Run ``enwiki.sh`` to obtain Wikipedia data. Note that this process is time-consuming and can take over 5 hours.