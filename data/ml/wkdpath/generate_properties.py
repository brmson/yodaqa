#!/usr/bin/python
#
# Generates all relations of given concept from Wikidata.
#
# Usage: data/ml/fbpath-emb/generate_relations.py INPUT OUTPUT

from SPARQLWrapper import SPARQLWrapper, JSON
import json, sys

url = 'https://query.wikidata.org/sparql'
input_file = sys.argv[1]
output_file = sys.argv[2]

def queryAllRelations(qid):
    sparql = SPARQLWrapper(url)
    sparql.setReturnFormat(JSON)
    sparql_query = '''
SELECT ?prop ?propLabel ?valresLabel WHERE {
  {  wd:''' + qid + ''' ?propres ?valres . }
  #XXX: This direction of relationship could produce a very large result set or lead to a query timeout
  #UNION
  #{    ?valres ?propres wd:''' + qid + ''' . }
  ?prop wikibase:directClaim ?propres .
         SERVICE wikibase:label {
     bd:serviceParam wikibase:language "cs"
  }
}'''
    sparql.setQuery(sparql_query)
    res = sparql.query().convert()
    result_map = {}
    for r in res['results']['bindings']:
        relation = r['prop']['value']
        label = r['propLabel']['value']
        value = r['valresLabel']['value']
        if (relation not in result_map):
            result_map[relation] = {"property": r['prop']['value'][len(PREFIX):], "label": label, "values": tuple([value])}
        else:
            tmp = list(result_map[relation]["values"])
            tmp.append(value)
            result_map[relation]["values"] = tuple(tmp)
    return list(result_map.values())

if __name__ == '__main__':
    PREFIX = "http://www.wikidata.org/entity/"
    with open(input_file) as f:
        dump = json.load(f)

    out_file = open(output_file, 'w')    

    out_file.write("[\n")
    for i, line in enumerate(dump):
        pageIds = [c['pageID'] for c in line['Concept']]
        labels = [c['fullLabel'] for c in line['Concept']]
        relations = []
        for p, lbl in zip(pageIds, labels):
            qid = p[len(PREFIX):]
            r = queryAllRelations(qid)
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
