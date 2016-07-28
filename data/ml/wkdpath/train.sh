#!/bin/bash

dataset=$1

./path_from_templates.py questions/main/ questions-reduced/main/train.json > questions-reduced/d-wikidata-rp/train.json
./path_from_templates.py questions/main/ questions-reduced/main/val.json > questions-reduced/d-wikidata-rp/val.json

${dataset}/scripts/fulldata.py train questions-reduced/full/ questions-reduced/main/ questions-reduced/d-dump/ questions-reduced/d-wikidata-rp/
${dataset}/scripts/fulldata.py val questions-reduced/full/ questions-reduced/main/ questions-reduced/d-dump/ questions-reduced/d-wikidata-rp/

../fbpath/fbpath_train_logistic.py questions-reduced/full/train.json questions-reduced/full/val.json