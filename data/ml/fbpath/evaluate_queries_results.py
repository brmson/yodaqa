#!/usr/bin/python -u
#
# Evaluate fbpath-based query performance (on gold standard and as predicted)
#
# Usage: evaluate_queries_results.py traindata.json valdata.json
#
# A model is trained on traindata and then its performance is measured
# on valdata.  (FIXME: Duplicate code with fbpath_train_logistic, instead
# of reusing the already-trained model.)
#
# The json data can be generated using:
#
#	mkdir -p data/ml/fbpath/wq-fbpath
#	cd ../dataset-factoid-webquestions
#	for i in trainmodel val devtest; do
#		scripts/fulldata.py $i ../yodaqa/data/ml/fbpath/wq-fbpath/ main/ d-dump/ d-freebase-mids/ d-freebase-brp/
#	done
#
# Example: data/ml/fbpath/evaluate_queries_results.py data/ml/fbpath/wq-fbpath/trainmodel.json data/ml/fbpath/wq-fbpath/val.json
#
# For every question, the script prints its qId, whether all answers were found
# using gold standard fbpaths, whether any answer was found using gold standard
# fbpaths, whether all answers were found using predicted fbpaths and whether
# any answer was found using predicted fbpaths.
#
# At the end of the script, it prints number of questions and percentual
# information about all/any answers obtained form freebase using gold
# standard/predicted fbpaths plus it prints number of questions which could not
# be answered because SPARQLWrapper does not support long queries.

from SPARQLWrapper import SPARQLWrapper, JSON
import json, sys
from fbpathtrain import VectorizedData
import random, time
from sklearn.linear_model import LogisticRegression
from sklearn.multiclass import OneVsRestClassifier
import numpy as np
from urllib2 import HTTPError

def check_q(cfier, v, i):
    probs = cfier.predict_proba(v.X.toarray()[i])[0]
    top_probs = sorted(enumerate(probs), key=lambda k: k[1], reverse=True)
    top_lprobs = ['%s: %.3f' % (v.Ydict.classes_[k[0]], k[1]) for k in top_probs[:15]]
    return (sorted(v.Xdict.inverse_transform(v.X[i])[0].keys(), key=lambda s: reversed(s)),
        v.Ydict.inverse_transform(cfier.predict(v.X.toarray()[i]))[0],
        top_lprobs,
        v.Ydict.inverse_transform(np.array([v.Y[i]]))[0])

def generate_query(paths, mid, proba, concepts):
    pathQueries = []
    for path in paths:
        path = [p[1:].replace("/",".") for p in path]
        if (len(path) == 1):
            pathQueryStr = "{" \
            "  ns:" + mid + " ns:" + path[0] + " ?val .\n" \
            "  BIND(\"ns:" + path[0] + "\" AS ?prop)\n" \
            "  BIND(" + proba + " AS ?score)\n" \
            "  BIND(0 AS ?branched)\n" \
            "  BIND(ns:" + mid + " AS ?res)\n" \
            "  OPTIONAL {\n" \
            "    ns:" + path[0] + " rdfs:label ?proplabel .\n" \
            "    FILTER(LANGMATCHES(LANG(?proplabel), \"en\"))\n" \
            "  }\n" \
            "}"
            pathQueries.append(pathQueryStr);
        elif (len(path) == 2):
            pathQueryStr = "{" \
            "  ns:" + mid + " ns:" + path[0] + "/ns:" + path[1] + " ?val .\n" \
            "  BIND(\"ns:" + path[0] + "/ns:" + path[1] + "\" AS ?prop)\n" \
            "  BIND(" + proba + " AS ?score)\n" \
            "  BIND(0 AS ?branched)\n" \
            "  BIND(ns:" + mid + " AS ?res)\n" \
            "  OPTIONAL {\n" \
            "    ns:" + path[0] + " rdfs:label ?pl0 .\n" \
            "    ns:" + path[1] + " rdfs:label ?pl1 .\n" \
            "    FILTER(LANGMATCHES(LANG(?pl0), \"en\"))\n" \
            "    FILTER(LANGMATCHES(LANG(?pl1), \"en\"))\n" \
            "    BIND(CONCAT(?pl0, \": \", ?pl1) AS ?proplabel)\n" \
            "  }\n" \
            "}"
            pathQueries.append(pathQueryStr);
        elif (len(path) == 3):
            for concept in concepts:
                witnessRel = path[2];
                quotedTitle = concept['fullLabel'].replace("\"", "").replace("\\\\", "").replace("\n", " ")
                pathQueryStr = "{" \
                "  ns:" + mid + " ns:" + path[0] + " ?med .\n" \
                "  ?med ns:" + path[1] + " ?val .\n" \
                "  {\n" \
                "    ?med ns:" + witnessRel + " ?concept .\n" \
                "    ?concept <http://rdf.freebase.com/key/wikipedia.en_id> \"" + concept['pageID'] + "\" .\n" \
                "  } UNION {\n" \
                "    {\n" \
                "      ?med ns:" + witnessRel + " ?wlabel .\n" \
                "      FILTER(!ISURI(?wlabel))\n" \
                "    } UNION {\n" \
                "      ?med ns:" + witnessRel + " ?concept .\n" \
                "      ?concept rdfs:label ?wlabel .\n" \
                "    }\n" \
                "    FILTER(LANGMATCHES(LANG(?wlabel), \"en\"))\n" \
                "    FILTER(CONTAINS(LCASE(?wlabel), LCASE(\"" + quotedTitle + "\")))\n" \
                "  }\n" \
                "  BIND(\"ns:" + path[0] + "/ns:" + path[1] + "\" AS ?prop)\n" \
                "  BIND(" + proba + " AS ?score)\n" \
                "  BIND(1 AS ?branched)\n" \
                "  BIND(ns:" + mid + " AS ?res)\n" \
                "  OPTIONAL {\n" \
                "    ns:" + path[0] + " rdfs:label ?pl0 .\n" \
                "    ns:" + path[1] + " rdfs:label ?pl1 .\n" \
                "    FILTER(LANGMATCHES(LANG(?pl0), \"en\"))\n" \
                "    FILTER(LANGMATCHES(LANG(?pl1), \"en\"))\n" \
                "    BIND(CONCAT(?pl0, \": \", ?pl1) AS ?proplabel)\n" \
                "  }\n" \
                "}"
                pathQueries.append(pathQueryStr)
    return pathQueries

def generate_results(paths, mids, concepts):
    prefix = """PREFIX owl: <http://www.w3.org/2002/07/owl#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX foaf: <http://xmlns.com/foaf/0.1/>
    PREFIX dc: <http://purl.org/dc/elements/1.1/>
    PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
    PREFIX ns: <http://rdf.freebase.com/ns/>
    SELECT ?property ?value ?prop ?val ?res ?score ?branched ?witnessAF WHERE {"""
    postfix = """BIND( IF(BOUND(?proplabel), ?proplabel, ?prop) AS ?property )
    OPTIONAL {
      ?val rdfs:label ?vallabel .
      FILTER( LANGMATCHES(LANG(?vallabel), "en") )
    }
    BIND( IF(BOUND(?vallabel), ?vallabel, ?val) AS ?value )
    FILTER( !ISURI(?value) )
    FILTER( LANG(?value) = "" || LANGMATCHES(LANG(?value), "en") )
     }LIMIT 400"""
    url = 'http://freebase.ailao.eu:3030/freebase/query'
    tmp = generate_query(paths, mids[0], "1", concepts)
    sparql = SPARQLWrapper(url)
    sparql.setReturnFormat(JSON)
    query = prefix + " UNION ".join(tmp) + postfix
    sparql.setQuery(query)
    res = sparql.query().convert()
    results = list(set([r['value']['value'] for r in res['results']['bindings']]))
    return results

if __name__ == '__main__':
    with open(sys.argv[1], 'r') as f:
        traindata = VectorizedData(json.load(f))
    print('// traindata: %d questions, %d features, %d fbpaths' % (
    np.size(traindata.X, axis=0), np.size(traindata.X, axis=1), np.size(traindata.Y, axis=1)))
    sys.stdout.flush()

    t_start = time.clock()
    cfier = OneVsRestClassifier(LogisticRegression(penalty='l1'), n_jobs=4)
    cfier.fit(traindata.X, traindata.Y)

    with open(sys.argv[2]) as f:
        full = json.load(f)
        full_data =  VectorizedData(full, traindata.Xdict, traindata.Ydict)
    error = 0
    anyCnt = 0
    allCnt = 0
    anyPCnt = 0
    allPCnt = 0
    for i, line in enumerate(full):    
        concepts = line['Concept']
        mids = [c["mid"] for c in line['freebaseMids']]
        relpaths = [c[0] for c in line['relPaths']]        
        predicted_paths = [lab.split(":")[0].split("|") for lab in check_q(cfier, full_data, i)[2]]
        try:
            results = generate_results(relpaths, mids, concepts)
            predicted_results = generate_results(predicted_paths, mids, concepts)
        except HTTPError:
            error += 1
            continue
        # print(results)
        allAnswers = True
        allAnswersPredicted = True
        anyAnswers = False
        anyAnswersPredicted = False

        for a in line["answers"]:
            if (a in results):
                anyAnswers = True
            else:
                allAnswers = False
            if (a in predicted_results):
                anyAnswersPredicted = True
            else:
                allAnswersPredicted = False
        if (anyAnswers):
            anyCnt += 1
        if (anyAnswersPredicted):
            anyPCnt += 1
        if (allAnswersPredicted):
            allPCnt += 1
        if (allAnswers):
            allCnt += 1
        print("qID %s, all: %s, all form predicted: %s, any: %s, any form predicted: %s" % (line['qId'], allAnswers, allAnswersPredicted, anyAnswers, anyAnswersPredicted))
print("SUMARRY")
print("Number of questions: %d, all: %f, all predicted: %f, any: %f, any predicted: %f, http error: %d" % 
    (len(full), (1.0*allCnt)/len(full), (1.0*allPCnt)/len(full), (1.0*anyCnt)/len(full), (1.0*anyPCnt)/len(full), error))
