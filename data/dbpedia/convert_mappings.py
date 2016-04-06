#!/usr/bin/python
# -*- coding: utf-8 -*-
"""
FILE         $Id$
AUTHOR       Alan Eckhardt <alan.eckhardt@firma.seznam.cz>
DESCRIPTION  Converts mapping from dbpedia to RDF triplets.

Copyright (c) 2016 Seznam.cz, a.s.
All rights reserved.

Get the mappings from

    https://github.com/dbpedia/extraction-framework/blob/master/mappings/Mapping_cs.xml

and then:

    convert_mappings.py --mappings Mapping_cs.xml
"""

from __future__ import unicode_literals
from __future__ import print_function
from __future__ import absolute_import
from __future__ import division

import sys
sys.path.insert(0,".")
import argparse
import codecs

def parse_args():
    parser = argparse.ArgumentParser(description='''
    Prepares necessary barrels for demo.
    ''')
    parser.add_argument('--mappings', type=str, required=True, help='Path to mappings.')
    return parser.parse_args()

CLASS_DEF="| mapToClass"
PROP_DEF="{{PropertyMapping | templateProperty = "
ONT_PROP="| ontologyProperty = "
prefixes = {
    "foaf":"<http://xmlns.com/foaf/0.1/",
    "dc":"<http://purl.org/dc/elements/1.1/",
    "geo":"<http://www.w3.org/2003/01/geo/wgs84_pos#",
    "rdfs":"<http://www.w3.org/2000/01/rdf-schema#",
    
    
}

def main():
    # Encode stdout to UTF8
    reload(sys)
    sys.setdefaultencoding('utf-8')

    args = parse_args()
    f = codecs.open(args.mappings, "r", "utf8")
    for_class = ""

    map1 = {}
    map2 = {}
    for line in f:
        line = line.strip()
        if line.startswith(CLASS_DEF):
            for_class = line[len(CLASS_DEF):]
        elif line.startswith(PROP_DEF):
            start = len(PROP_DEF)
            end = start + line[len(PROP_DEF):].find("|")
            # Czech property
            prop = line[start:end].strip()
            #prop = "<http://cs.dbpedia.org/property/"+prop+">"
            # Ontology property
            ont_prop = line[end+len(ONT_PROP):-3].strip()
            if "|" in ont_prop:
                ont_prop = ont_prop[:ont_prop.find(" |")-2]


            if ":" not in ont_prop:
                ont_prop = "<http://dbpedia.org/ontology/"+ont_prop+">"
            else:
                ont_prop = prefixes[ont_prop[:ont_prop.find(":")]]+ont_prop[ont_prop.find(":")+1:]+">"
            if ont_prop in map1 and map1[ont_prop] != prop:
                print("Error for",for_class, ont_prop.encode("utf8"), prop.encode("utf8"), repr(map1[ont_prop]).encode("utf8"))
            if prop in map2 and map2[prop] != ont_prop:
                print("Error2 for", ont_prop.encode("utf8"), prop.encode("utf8"), repr(map2[prop]).encode("utf8"))

            if ont_prop not in map1:
                map1[ont_prop] = set()
            map1[ont_prop].add(prop)
            if prop not in map2:
                map2[prop] = set()
            map2[prop].add(ont_prop)
    
    f_w = codecs.open(args.mappings+".n3", "w", "utf8")
    for ont_prop in map1:
        for prop in map1[ont_prop]:
            f_w.write(" ".join([ont_prop, "<http://www.w3.org/2000/01/rdf-schema#label>", '"'+prop+'"@cs .'])+"\n")
    #endfor writing
    f_w.close()
    f.close()
#enddef

# entrypoint
if __name__ == '__main__':
    main()
