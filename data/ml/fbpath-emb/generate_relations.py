#!/usr/bin/python3
#
# Generates all relations of given concept or all relations of concept which is connect with given concept by gold standard relation. 
#
# Usage: generate_relations.py RELATION_NUM SPLIT_NAME MID_DIR [1REL_DIR] OUTDIR
#
# Example:
# data/ml/fbpath-emb/generate_relations.py 1 trainmodel ../dataset-factoid-webquestions/d-freebase-mids/ data/ml/fbpath-emb/relations/
# OR
# data/ml/fbpath-emb/generate_relations.py 2 trainmodel ../dataset-factoid-webquestions/d-freebase-mids/ data/ml/fbpath-emb/relations/ data/ml/fbpath-emb/relations2/
#
# This uses the https://github.com/brmson/dataset-factoid-webquestions dataset.
#
# RELATION_NUM - 1 to generate concept relations, 2 to generate relations of connected concept
# SPLIT_NAME   - one fo the [devtest, trainmodel, test, val]
# MID_DIR      - directory name cotaining mids of entity mids for each question
# 1REL_DIR     - directory containing first level relations
# OUTDIR       - output directory

from SPARQLWrapper import SPARQLWrapper, JSON
import json, sys

url = 'http://freebase.ailao.eu:3030/freebase/query'
relation_num = sys.argv[1]
split_name = sys.argv[2]
in_folder = sys.argv[3]
if (relation_num == '1' or relation_num == 'all'):
    out_folder = sys.argv[4]
elif (relation_num == '2'):
    relations_folder = sys.argv[4]
    out_folder = sys.argv[5]
else:
    exit("Wrong relation number. Must be 1, 2 or all")

def queryAllRelations(mid):
    try:
        with open('data/ml/fbpath-emb/fbconcepts-custom/' + mid + '.json') as f:
            print("Loaded concept %s from file." % (mid,))
            res = json.load(f)
    except IOError:        
        print("Creating query for concept %s." % (mid,))
        sparql = SPARQLWrapper(url)
        sparql.setReturnFormat(JSON)
        sparql_query = '''
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX ns: <http://rdf.freebase.com/ns/>
    SELECT DISTINCT ?prop ?label WHERE { 
    ns:''' + mid + ''' ?prop ?value .
    FILTER( STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/') ) .    
    OPTIONAL {
      ?prop rdfs:label ?label .
      FILTER( LANGMATCHES(LANG(?label), "en") ) 
    } }''' 
    # FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/freebase') )
    # FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/media_common.quotation') )
    # FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/user') )
    # FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/base') )
    # FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/topic_server') )
    # } '''
    # FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/type') )
    # FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/common') )
        sparql.setQuery(sparql_query)
        res = sparql.query().convert()
        with open('data/ml/fbpath-emb/fbconcepts-custom/' + mid + '.json', 'w') as f:
            json.dump(res, f)
    retVal = []
    for r in res['results']['bindings']:
        relation = r['prop']['value']
        if ('label' not in r):
            print('Warning! Property %s has no label. Using last segement of property name.' % (r['prop']['value'][27:],))
            label = r['prop']['value'][27:].split('.')[-1].replace('_', ' ')
        else:
            label = r['label']['value']
        retVal.append({"property": r['prop']['value'][27:], "label": label})
    return retVal

def leadsToCVT(mid, relation):
    file_name = mid + '#' + relation
    print("Creating query for %s." % (file_name,))
    sparql = SPARQLWrapper(url)
    sparql.setReturnFormat(JSON)
    sparql_query = '''
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX ns: <http://rdf.freebase.com/ns/>
SELECT DISTINCT ?meta ?label WHERE { 
ns:''' + mid + ''' ns:''' + relation + ''' ?meta .
FILTER( REGEX(STR(?meta), '^http://rdf.freebase.com/ns/m\\\.') )
} LIMIT 1'''
    sparql.setQuery(sparql_query)
    res = sparql.query().convert()
    if (res['results']['bindings'] == []):
        return False
    sparql_query = '''
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX ns: <http://rdf.freebase.com/ns/>
SELECT DISTINCT ?meta ?label WHERE { 
ns:''' + mid + ''' ns:''' + relation + ''' ?meta .
FILTER( REGEX(STR(?meta), '^http://rdf.freebase.com/ns/m\\\.') )
?meta ns:type.object.name ?label .
FILTER( LANGMATCHES(LANG(?label), "en") )    
} LIMIT 1'''
    sparql.setQuery(sparql_query)
    res = sparql.query().convert()
    if (res['results']['bindings'] == []):
        return True
    return False

def querySecondLevel(m, rel):
    file_name = m + '#' + rel
    try:
        with open('data/ml/fbpath-emb/fbconcepts-custom/meta-nodes/' + file_name + '.json') as f:
            print("Loaded concept %s from file." % (file_name,))
            res = json.load(f)
    except IOError: 
        print(m, rel)
        sparql = SPARQLWrapper(url)
        sparql.setReturnFormat(JSON)
        sparql_query = '''
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX ns: <http://rdf.freebase.com/ns/>
    SELECT DISTINCT ?prop ?label WHERE { 
    ns:''' + m + ' ns:' + rel + ''' ?meta .
    FILTER( STRSTARTS(STR(?meta), 'http://rdf.freebase.com/ns/') ) .
    ?meta ?prop ?ans .
    FILTER( STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/') ) .    
    OPTIONAL {
      ?prop rdfs:label ?label .
      FILTER( LANGMATCHES(LANG(?label), "en") ) 
    } }''' 
        sparql.setQuery(sparql_query)
        res = sparql.query().convert()
        with open('data/ml/fbpath-emb/fbconcepts-custom/meta-nodes/' + file_name + '.json', 'w') as f:
            json.dump(res, f)
    retVal = []
    for r in res['results']['bindings']:
        relation = r['prop']['value']
        if ('label' not in r):
            print('Warning! Property %s has no label. Using last segement of property name.' % (r['prop']['value'][27:],))
            label = r['prop']['value'][27:].split('.')[-1].replace('_', ' ')
        else:
            label = r['label']['value']
        retVal.append({"property": r['prop']['value'][27:], "label": label})
    print(retVal)
    return retVal    

def queryWitRel(mid, prop, wit_prop, other_c):
    # file_name = mid + '#' + relation
    # print("Creating query for %s." % (file_name,))
    sparql = SPARQLWrapper(url)
    sparql.setReturnFormat(JSON)
    sparql_query = '''
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX ns: <http://rdf.freebase.com/ns/>
SELECT DISTINCT ?meta WHERE { 
ns:''' + mid + ''' ns:''' + prop + ''' ?meta .
FILTER( REGEX(STR(?meta), '^http://rdf.freebase.com/ns/m\\\.') )
{
    ?meta ns:''' + wit_prop + ''' ns:''' + other_c['mid'] + ''' .
} UNION {
{
    ?meta ns:''' + wit_prop + ''' ?wlabel .
    FILTER(!ISURI(?wlabel))
} UNION {
    ?meta ns:''' + wit_prop + ''' ?concept .
    ?concept rdfs:label ?wlabel .
}
FILTER(LANGMATCHES(LANG(?wlabel), "en"))
FILTER(CONTAINS(LCASE(?wlabel), LCASE("''' + other_c['concept'] + '''")))
} }'''   
    sparql.setQuery(sparql_query)
    res = sparql.query().convert()
    if (res['results']['bindings'] == []):
        return False
    return True

def make_paths_map(paths):
    res = {}
    for line in paths:
        res[line['qId']] = line['allRelations']
    return res 

def remove_duplicates(entity_paths):
    myset = set()
    res = []
    for ent_path in entity_paths:
        # print(ent_path['path'])
        p = tuple([x['property'] for x in ent_path['path']])
        # print(p)
        if (p not in myset):
            myset.add(p)
            res.append(ent_path)
            
    return res

if __name__ == '__main__':    
    with open(in_folder + "/" + split_name + ".json") as f:
        dump = json.load(f)

    if (relation_num == 2):
        with open(relations_folder + "/" + split_name + ".json") as f:
            paths = json.load(f)
        path_map = make_paths_map(paths) 

    out_file = open(out_folder + "/" + split_name + ".json", 'w')       

    try:
        with open('data/ml/fbpath-emb/fbconcepts-custom/cvt_map.json') as f:
            cvt_map = json.load(f) 
    except IOError:     
        cvt_map = {}
    out_file.write("[\n")
    try:
        for i, line in enumerate(dump):
            print(line['qId'])
            mids = [c['mid'] for c in line['freebaseMids']]
            relations = []
            for m in mids:
                if (m == None or m == ''):
                    continue
                if (relation_num == 'all'):
                    rel_one = queryAllRelations(m)
                    for r in rel_one:
                        if (r['property'] not in cvt_map):
                            is_cvt = leadsToCVT(m, r['property'])
                            cvt_map[r['property']] = is_cvt
                        is_cvt = cvt_map[r['property']]
                        if (not is_cvt):
                            relations.append({'path':(r,), 'entities':[m]})
                            continue                    
                        rel_two = querySecondLevel(m, r['property'])
                        for r2 in rel_two:
                            for c in line['freebaseMids']:
                                if (c['mid'] == m):
                                    continue
                                is_wit = queryWitRel(m, r['property'], r2['property'], c)
                                if (is_wit):
                                    relations.extend([{'path':(r, rm, r2), 'entities':[m, c['mid']]} for rm in rel_two if rm['property'] != r2['property']])
                                else: 
                                    relations.append({'path':(r, r2), 'entities':[m]})
                elif (relation_num == '1'):
                    relations = queryAllRelations(m)
                    for r in relations:
                        if (r['property'] not in cvt_map):
                            is_cvt = leadsToCVT(m, r['property'])
                            cvt_map[r['property']] = is_cvt
                        r['is_cvt'] = cvt_map[r['property']]
                    s = set([tuple(rel.items()) for rel in relations])
                    relations = [dict(rel) for rel in s]
                else:
                    qmids = []
                    path_list = path_map[line['qId']]
                    for rel in path_list:
                        if (not rel['is_cvt']):
                            continue                    
                        relations.extend(querySecondLevel(m, rel['property']))
                    s = set([tuple(rel.items()) for rel in relations])
                    relations = [dict(rel) for rel in s]
            # print (relations)
            # print (relations == remove_duplicates(relations))
            if (relation_num == 'all'):
                relations = remove_duplicates(relations)
            res = {}
            res['qId'] = line['qId']
            res['allRelations'] = relations
            # print(json.dumps(res))
            # break
            if (i+1 != len(dump)):
                out_file.write(json.dumps(res) + ",\n")
            else:
                out_file.write(json.dumps(res) + "\n")
    except:
        raise
    finally:
        with open('data/ml/fbpath-emb/fbconcepts-custom/cvt_map.json', 'w') as f:
            json.dump(cvt_map, f)        
    out_file.write("]\n")
    out_file.close()
