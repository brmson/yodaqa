#!/bin/bash

username=$(whoami)
# Create directory for the data
mkdir /home/$username/docker/data/db
cp setup-jena.sh /home/$username/docker/data/db
cp fix-nt.sh /home/$username/docker/data/db
cd /home/$username/docker/data/db

# Download files
wget http://downloads.dbpedia.org/2014/dbpedia_2014.owl.bz2
wget http://downloads.dbpedia.org/2014/en/labels_en.nt.bz2
wget http://downloads.dbpedia.org/2014/en/redirects_transitive_en.nt.bz2
wget http://downloads.dbpedia.org/2014/en/page_ids_en.nt.bz2
wget http://downloads.dbpedia.org/2014/en/instance_types_en.nt.bz2
wget http://downloads.dbpedia.org/2014/en/instance_types_heuristic_en.nt.bz2
wget http://downloads.dbpedia.org/2014/links/wordnet_links.nt.bz2
wget http://downloads.dbpedia.org/2014/links/yago_types.nt.bz2
wget http://downloads.dbpedia.org/2014/links/yago_taxonomy.nt.bz2
wget http://downloads.dbpedia.org/2014/en/mappingbased_properties_cleaned_en.nt.bz2
wget http://downloads.dbpedia.org/2014/en/specific_mappingbased_properties_en.nt.bz2
wget http://downloads.dbpedia.org/2014/en/infobox_properties_en.nt.bz2
wget http://downloads.dbpedia.org/2014/en/infobox_property_definitions_en.nt.bz2
wget http://downloads.dbpedia.org/2014/en/disambiguations_en.nt.bz2
wget http://downloads.dbpedia.org/2014/en/short_abstracts_en.nt.bz2

# Extract
bunzip2 -k *.bz2
rm *.bz2

# Fix errors in one of the files
/bin/bash fix-nt.sh

# Download Apache Jena and Fuseki
wget http://apache.miloslavbrada.cz/jena/binaries/apache-jena-3.1.0.tar.gz
wget http://apache.miloslavbrada.cz/jena/binaries/apache-jena-fuseki-2.4.0.tar.gz

# Setup Jena
/bin/bash setup-jena.sh

cd /
