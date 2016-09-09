#!/bin/bash
mkdir /home/fp/docker/data/enwiki
cd /home/fp/docker/data/enwiki

# Clone extractor repo
git clone https://github.com/brmson/wikipedia-extractor

# Download data
wget http://rover.ms.mff.cuni.cz/~pasky/brmson/enwiki-20150112-pages-articles.xml.bz2

# Extract data; Runs for about 5 hours
mkdir enwiki-text
bzcat enwiki*.bz2 | wikipedia-extractor/WikiExtractor.py -o enwiki-text -H -c -x

# Create xml
bin/extracted2xml.sh enwiki-text.xml

cd /
