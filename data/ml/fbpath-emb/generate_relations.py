#!/usr/bin/python
#
# Generates all relations of given concept or all relations of concept which is connect with given concept by gold standard relation. 
#
# Usage: generate_relations.py RELATION_NUM SPLIT_NAME DUMP_DIR [GS_DIR] OUTDIR
#
# Example:
# data/ml/fbpath-emb/generate_relations.py 1 trainmodel ../dataset-factoid-webquestions/d-dump/ data/ml/fbpath-emb/relations/
# OR
# data/ml/fbpath-emb/generate_relations.py 2 trainmodel ../dataset-factoid-webquestions/d-dump/ ../dataset-factoid-webquestions/d-freebase-brp/ data/ml/fbpath-emb/relations2/
#
# This uses the https://github.com/brmson/dataset-factoid-webquestions dataset.
#
# RELATION_NUM - 1 to generate concept relations, 2 to generate relations of connected concept
# SPLIT_NAME   - one fo the [devtest, trainmodel, test, val]
# DUMP_DIR     - directory name of yodaqa question dump
# GS_DIR       - directory of goldstandard freebase paths
# OUTDIR       - output directory

from SPARQLWrapper import SPARQLWrapper, JSON
import json, sys

url = 'http://freebase.ailao.eu:3030/freebase/query'
relation_num = int(sys.argv[1])
split_name = sys.argv[2]
in_folder = sys.argv[3]
if (relation_num == 1):
    out_folder = sys.argv[4]
elif (relation_num == 2):
    relations_folder = sys.argv[4]
    out_folder = sys.argv[5]
else:
    exit("Wrong relation number. Must be 1 or 2")

def queryFreebasekey(page_id):    
    sparql = SPARQLWrapper(url)
    sparql.setReturnFormat(JSON)
    sparql_query = '''
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX ns: <http://rdf.breebase.com/ns/>
SELECT DISTINCT ?topic WHERE { 
?topic <http://rdf.freebase.com/key/wikipedia.en_id> "''' + page_id + '''" .
} '''
    sparql.setQuery(sparql_query)
    res = sparql.query().convert()
    retVal = []
    for r in res['results']['bindings']:
        retVal.append(r['topic']['value'][27:])
    return retVal

def queryLabel(relation):
    sparql = SPARQLWrapper(url)
    sparql.setReturnFormat(JSON)
    sparql_query = '''
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX ns: <http://rdf.freebase.com/ns/>
SELECT DISTINCT ?label WHERE { 
<''' + relation + '''> rdfs:label ?label .
FILTER( LANGMATCHES(LANG(?label), "en") )
} '''
    sparql.setQuery(sparql_query)
    res = sparql.query().convert()
    s = set()
    retVal = []
    for r in res['results']['bindings']:
        retVal.append(r['label']['value'])
    return retVal[0] if len(retVal) > 0 else ""

def queryAllRelations(mid):
    sparql = SPARQLWrapper(url)
    sparql.setReturnFormat(JSON)
    # The filters come from FreebaseOntology.java
    sparql_query = '''
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX ns: <http://rdf.freebase.com/ns/>
SELECT DISTINCT ?prop WHERE { 
ns:''' + mid + ''' ?prop ?value .
FILTER( STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/') )
FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/type') )
FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/common') )
FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/freebase') )
FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/media_common.quotation') )
FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/user') )
FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/base') )
FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/topic_server') )
} '''
    sparql.setQuery(sparql_query)
    res = sparql.query().convert()
    retVal = []
    for r in res['results']['bindings']:
        relation = r['prop']['value']
        label = queryLabel(relation)
        retVal.append({"relation": r['prop']['value'][27:], "label": label})
    return retVal

def queryMetaNode(page_id, relation):    
    sparql = SPARQLWrapper(url)
    sparql.setReturnFormat(JSON)
    sparql_query = '''
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX ns: <http://rdf.freebase.com/ns/>
SELECT DISTINCT ?meta WHERE { 
?topic <http://rdf.freebase.com/key/wikipedia.en_id> "''' + page_id + '''" .
?topic ns:''' + relation + ''' ?meta .
FILTER( REGEX(STR(?meta), '^http://rdf.freebase.com/ns/m\\\.') )
} '''
    sparql.setQuery(sparql_query)
    res = sparql.query().convert()
    retVal = []
    for r in res['results']['bindings']:
        retVal.append(r['meta']['value'][27:])
    return retVal

with open(in_folder + "/" + split_name + ".json") as f:
    dump = json.load(f)

if (relation_num == 2):
    with open(relations_folder + "/" + split_name + ".json") as f:
        paths = json.load(f)
    zipped = zip(dump,paths)
else:
    zipped = dump

    
out_file = open(out_folder + "/" + split_name + ".json", 'w')    
out_file_c = open(out_folder + "/concept-relations/" + split_name + ".json", 'w')    

out_file.write("[\n")
for i, zipped_line in enumerate(zipped):
    if (relation_num == 1):
        line = zipped_line
    else:
        line, rel_line = zipped_line
    pageIds = [c['pageID'] for c in line['Concept']]
    labels = [c['fullLabel'] for c in line['Concept']]
    relations = []
    for p, lbl in zip(pageIds, labels):
        if (relation_num == 1):
            mids = queryFreebasekey(p)
        else:
            mids = []
            path_list = [rp[0][0] for rp in rel_line['relPaths']]
            path_list = [rp[1:].replace('/','.') for rp in path_list]
            for rel in path_list:
                mids.extend(queryMetaNode(p, rel))
            mids = list(set(mids))
        r = []
        for m in mids:
            r.extend(queryAllRelations(m))
        concept_relations = {}
        concept_relations['fullLabel'] = lbl
        concept_relations['pageID'] = p
        concept_relations['allRelations'] = r
        out_file_c.write(json.dumps(concept_relations) + "\n")
        relations.extend(r)
    s = set([tuple(rel.items()) for rel in relations])
    relations = [dict(rel) for rel in s]
    res = {}
    res['qId'] = line['qId']
    res['allRelations'] = relations
    print(json.dumps(res))
    if (i+1 != len(dump)):
        out_file.write(json.dumps(res) + ",\n")
    else:
        out_file.write(json.dumps(res) + "\n")
    
out_file.write("]\n")
out_file.close()
out_file_c.close()
