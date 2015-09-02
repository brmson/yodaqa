import xml.etree.ElementTree as ET
import sys, csv

tsv = sys.argv[1]
answers = {}
with open(tsv) as f:
    for line in csv.reader(f, delimiter='\t',skipinitialspace=True):
        answers[line[0]] = line[13].split(":")[0]

filename = sys.argv[2]
tree = ET.parse(filename)
root = tree.getroot()
elems = tree.findall(".//answer_section")
for e in elems:
    id = e.find("./question_id").text
    if (id in answers):
        e.find(".//expression").text = answers[id]

tree.write(sys.stdout, encoding="utf-8")