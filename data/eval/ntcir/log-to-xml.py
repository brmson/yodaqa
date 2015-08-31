from __future__ import print_function 
import xml.etree.cElementTree as ET
import math

LOGFILE="ntcirlog.txt"


IR_RESULT_SET=ET.Element("IR_RESULT_SET")


qid=""
    
for line in open(LOGFILE):
    if "NTCIRNEXTQUESTION" in line:
        qid=line.split(":")[-1]
        IR_RESULT=ET.SubElement(IR_RESULT_SET,"IR_RESULT",TOPIC_ID=qid,
                            QUERY_ID="",FILE_NAME="")
#        print ("ID",qid)
        QUERY=ET.SubElement(IR_RESULT,"QUERY")
        KEY_TERM_SET=ET.SubElement(QUERY,"KEY_TERM_SET",LANGUAGE="EN")
        DOCUMENT_SET=ET.SubElement(IR_RESULT,"DOCUMENT_SET")
        rank=1
    elif "NTCIRPRINT" in line:
        info=line.split(":")[-1].split("+")
#        print("INFO",info)
        score=str(1/(1+math.exp(-float(info[-1]))))
#        print("SCORE",score)
        ET.SubElement(KEY_TERM_SET,"KEY_TERM",RANK=str(rank),SCORE=score).text=info[1]
        ET.SubElement(DOCUMENT_SET,"DOCUMENT",RANK=str(rank),SCORE=score,SOURCE_ID="http://en.wikipedia.org/?curid="+info[0])
        rank+=1
    
xml=ET.ElementTree(IR_RESULT_SET)
xml.write("IRres.xml")
