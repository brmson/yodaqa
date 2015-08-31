from __future__ import print_function 
import xml.etree.cElementTree as ET
import sys
import json


jfile=open(sys.argv[1])
xmlpath=sys.argv[2]


TOPIC_SET=ET.Element("TOPIC_SET")
METADATA=ET.SubElement(TOPIC_SET,"METADATA")
ET.SubElement(METADATA,"RUN_ID").text="run_id here"
ET.SubElement(METADATA,"DESCRIPTION").text="yodaqa"
ANSWER_TYPE_SET_DEF=ET.SubElement(METADATA,"ANSWER_TYPE_SET_DEF")
ET.SubElement(ANSWER_TYPE_SET_DEF,"PATH").text="path to answers"
QUESTION_FORMAT_TYPE_DEF=ET.SubElement(METADATA,"ANSWER_TYPE_SET_DEF")
ET.SubElement(QUESTION_FORMAT_TYPE_DEF,"PATH").text="path to questions"

types=set()

for line in jfile:
#    print ("LINE",line)
    jline=json.loads(line)
    TOPIC=ET.SubElement(TOPIC_SET,"TOPIC",ID=jline['qId'])
    QUESTION_ANALYSIS=ET.SubElement(TOPIC,"QUESTION_ANALYSIS")
    QUESTION_FORMAT_TYPE_SET=ET.SubElement(QUESTION_ANALYSIS,"QUESTION_FORMAT_TYPE_SET")
    ET.SubElement(QUESTION_FORMAT_TYPE_SET,"QUESTION_FORMAT_TYPE",RANK="",SCORE="")
    ANSWER_TYPE_SET=ET.SubElement(QUESTION_ANALYSIS,"ANSWER_TYPE_SET")
    jtypes=jline['LAT']
    for i in range(0,len(jtypes)):
        if jtypes[i]['specificity']=='0.0':
            ET.SubElement(ANSWER_TYPE_SET,"ANSWER_TYPE",RANK="",SCORE="").text=jtypes[i]['text']
            types.add(jtypes[i]['text'])
    QUERY_SET=ET.SubElement(QUESTION_ANALYSIS,"QUERY_SET")
    QUERY=ET.SubElement(QUERY_SET,"QUERY",ID="1")
    KEY_TERM_SET=ET.SubElement(QUERY,"KEY_TERM_SET",LANGUAGE="EN")
    jclues=jline['Clue']
    for i in range(0,len(jclues)):
        score=float(jclues[i]['clueWeight'])
        if score<1:
            score=1.0
        ET.SubElement(KEY_TERM_SET,"KEY_TERM",RANK=str(i+1),SCORE=str(1-1/score)).text=jclues[i]['label']
xml=ET.ElementTree(TOPIC_SET)
xml.write(xmlpath)


ANSWER_TYPE_SET_DEF=ET.Element("ANSWER_TYPE_SET_DEF")

for t in types:
    ET.SubElement(ANSWER_TYPE_SET_DEF,"ANSWER_TYPE_DEF",PARENT="ROOT",CHILDREN="$").text=t
xmldef=ET.ElementTree(ANSWER_TYPE_SET_DEF)
xmldef.write(sys.argv[3])


#<ANSWER_TYPE_SET_DEF>
#  <ANSWER_TYPE_DEF PARENT="ROOT" CHILDREN="$">DEFINITION</ANSWER_TYPE_DEF>
#  <ANSWER_TYPE_DEF PARENT="ROOT" CHILDREN="$">PERSON</ANSWER_TYPE_DEF>
#  <ANSWER_TYPE_DEF PARENT="ROOT" CHILDREN="$">LOCATION</ANSWER_TYPE_DEF>
#</ANSWER_TYPE_SET_DEF>
#gs_root = ET.parse(sys.argv[2])
#answer_map = {}
#answers = gs_root.findall(".//answer_section")
#for a in answers:
#    ans = a.findall("answer_set/answer/expression_set/expression")
#    id = a.findall("question_id")[0].text
#    answer_map[id] = ans[0].text
#
#root = ET.parse(sys.argv[1])
#questions = root.findall(".//question")
#for q in questions:
#    if (q.get("minimal") == "yes"):
#    	s = q.get("id")[1:] + "\t" + q.get("answer_type") + "\t" + q.findall("instruction")[0].text.replace("\n","") + "\t" + str(answer_map[q.get("id")])
#        print (s.encode('utf8'))
#{"qId": "1669", "SV": [], "LAT": [{"synset": "4923519", "text": "property", "specificity": "-2.0", "type": "WordnetLAT"}, {"synset": "5009517", "text": "stature", "specificity": "0.0", "type": "WordnetLAT"}, {"synset": "24444", "text": "attribute", "specificity": "-3.0", "type": "WordnetLAT"}, {"synset": "5005153", "text": "bodily property", "specificity": "-1.0", "type": "WordnetLAT"}, {"text": "tall", "specificity": "0.0", "type": "LAT"}], "Clue": [{"label": "stature", "clueWeight": "1.5"}, {"label": "tall", "clueWeight": "1.5"}, {"label": "Mount McKinley", "clueWeight": "2.8000000000000003"}]"Concept": [{"fullLabel": "Mount McKinley", "cookedLabel": "Mount McKinley", "pageID": "207247"}]}