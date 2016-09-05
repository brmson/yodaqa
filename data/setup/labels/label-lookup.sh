#!/bin/bash
# Create directory if does not exist
mkdir /home/fp/docker/data
cd /home/fp/docker/data

# Clone repo
git clone https://github.com/brmson/label-lookup.git

cd label-lookup

# Download data
wget http://downloads.dbpedia.org/2014/en/labels_en.nt.bz2
wget http://downloads.dbpedia.org/2014/en/page_ids_en.nt.bz2
wget http://downloads.dbpedia.org/2014/en/redirects_transitive_en.nt.bz2

# Prepare data
./preprocess.py labels_en.nt page_ids_en.nt redirects_transitive_en.nt sorted_list.dat

# Setup Sqlite lookup; disable with comment if not necessary
/bin/bash lookup-lite.sh

# Rename directory with data
cd ..
rename label-lookup labels

# Return
cd /

