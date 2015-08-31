from __future__ import print_function 
import xml.etree.cElementTree as ET
import os
import csv

CSVFOLDER="csvtest"


IR_RESULT_SET=ET.Element("IR_RESULT_SET")

for file in os.listdir(CSVFOLDER):
    path=CSVFOLDER+"/"+file
    print ("FILE",file.split(".csv")[0])
    IR_RESULT=ET.SubElement(IR_RESULT_SET,"IR_RESULT",TOPIC_ID=file.split(".csv",1)[0],
                            QUERY_ID="",FILE_NAME="")
    QUERY=ET.SubElement(IR_RESULT,"QUERY")
    KEY_TERM_SET=ET.SubElement(QUERY,"KEY_TERM_SET",LANGUAGE="EN")
    DOCUMENT_SET=ET.SubElement(IR_RESULT,"DOCUMENT_SET")    
    i=0    
    for line in csv.reader(open(path), delimiter=',',skipinitialspace=True):
        if(i>0):
#            print("LINE",line)
            ET.SubElement(KEY_TERM_SET,"KEY_TERM",RANK=str(i),SCORE=line[2]).text=line[0]
            ET.SubElement(DOCUMENT_SET,"DOCUMENT",RANK=str(i),SCORE="???",SOURCE_ID="??")
        i+=1
        
xml=ET.ElementTree(IR_RESULT_SET)
xml.write("IRres.xml")
