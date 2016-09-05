#!/bin/bash
# Create directory if does not exist
mkdir /home/fp/docker/data
cp lookup-lite.sh /home/fp/docker/data
cd /home/fp/docker/data

# Clone repo
git clone https://github.com/brmson/label-lookup.git

cd label-lookup

# Setup Sqlite lookup
mv ../lookup-lite.sh .
/bin/bash lookup-lite.sh

# Rename directory with data
cd ..
rename label-lookup labels

# Return
cd /

