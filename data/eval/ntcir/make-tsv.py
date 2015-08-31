from __future__ import print_function 
import xml.etree.ElementTree as ET
import sys

def itertext(self):
    tag = self.tag
    if not isinstance(tag, str) and tag is not None:
        return
        if self.text:
            yield self.text
        for e in self:
            for s in e.itertext():
                yield s
            if e.tail:
                yield e.tail

if (len(sys.argv) > 2):
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
	if (len(sys.argv) > 2):
		answer = answer_map[q.get("id")]
	else:
		answer = "dummy answer"
	if (q.get("minimal") == "yes" and q.get("answer_style") == "description_unlimited"):
		try:
			text = ''.join(q.findall("instruction")[0].itertext()).replace("\n","")
		except IndexError:
			text = ''.join(q.itertext()).replace("\n","")
		s = q.get("id") + "\t" + q.get("answer_type") + "\t" + text + "\t" + str(answer)
		print (s.encode('utf8'))