from __future__ import print_function 
import xml.etree.cElementTree as ET
import csv

TSVFILE="ntcir-test-ovt-u6164128.tsv"
XMLFILE="QAres.xml"
answerTable=ET.Element("AnswerTable",filename="filename here")
    
    
for line in csv.reader(open(TSVFILE), delimiter='\t',skipinitialspace=True):
    print ("ID",line[0].split("_")[1])
    data=ET.SubElement(answerTable,"data")
    ET.SubElement(data,"answer").text=line[13].split(":")[0]
    ET.SubElement(data,"anscolumn_ID").text=line[0]
    
xml=ET.ElementTree(answerTable)
xml.write(XMLFILE)
