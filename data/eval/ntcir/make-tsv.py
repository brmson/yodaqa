from __future__ import print_function 
import xml.etree.ElementTree as ET
import sys

gs_root = ET.parse(sys.argv[2])
answer_map = {}
answers = gs_root.findall(".//answer_section")
for a in answers:
    ans = a.findall("answer_set/answer/expression_set/expression")
    id = a.findall("question_id")[0].text
    answer_map[id] = ans[0].text

root = ET.parse(sys.argv[1])
questions = root.findall(".//question")
for q in questions:
    if (q.get("minimal") == "yes" and q.get("answer_style") == "description_unlimited"):
    	s = q.get("id")[1:] + "\t" + q.get("answer_type") + "\t" + q.findall("instruction")[0].text.replace("\n","") + "\t" + str(answer_map[q.get("id")])
        print (s.encode('utf8'))