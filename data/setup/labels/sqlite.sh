#!/bin/bash
username=$(whoami)
# Create directory if does not exist
mkdir /home/$username/docker/data
cp lookup-lite.sh /home/$username/docker/data
cd /home/$username/docker/data

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

