#!/usr/bin/python
#
# Script that sends questions from JSON file to the YodaQA web frontend
# REST API and prints results for error evaluation+analysis.
#
# Argument 1: JSON filename
# Argument 2: YodaQA URL (either "http://localhost:4567" or "http://qa.ailao.eu:4000")

import requests
import json
import sys
from time import sleep

def byteify(input):
    if isinstance(input, dict):
        return {byteify(key):byteify(value) for key,value in input.iteritems()}
    elif isinstance(input, list):
        return [byteify(element) for element in input]
    elif isinstance(input, unicode):
        return input.encode('utf-8')
    else:
        return input

argv = sys.argv
filename = argv[1]
URL = argv[2]
json_data = open(filename)
parsed_data = byteify(json.load(json_data))
number_of_questions = len(parsed_data)
question_counter = 0
correctly_answered = 0
recall = 0
finished = False

print('%04s\t%.50s\t%.10s\t%.15s\t%.15s\t%s' % ("ID", "Question Text".ljust(50), "indicator", "correct answer".ljust(15), "found".ljust(15), "URL"))

while question_counter < number_of_questions:
    questionText = parsed_data[question_counter]["qText"]
    questionAnswer = parsed_data[question_counter]["answers"][0]
    ID = parsed_data[question_counter]["qID"]
    finished = False
    indicator = "incorrect"
    r = requests.post(URL+"/q", data={'text':questionText} )
    current_qID = byteify(r.json()["id"])
    while (finished == False): #wait for web interface to finish
        sleep(0.5)
        data = requests.get(URL +"/q/"+ current_qID).json()
        finished = data["finished"]

    answer_list = byteify(data["answers"])
    for i in range (0, len(answer_list)): #iterate through answers and look for our correct one
        if (questionAnswer == answer_list[i]['text']): 
            if (i == 0):
                correctly_answered += 1
                indicator = "correct  "
                continue
            else:
                recall	 += 1
                indicator = "recall   "
                continue
    print('%03s\t%.50s\t%.10s\t%.15s\t%.15s\t%s' % (ID, questionText.ljust(50),indicator, questionAnswer.ljust(15), answer_list[0]['text'].ljust(15), (URL+"/q/"+str(current_qID))))
    question_counter += 1

print("correctly answered: " + str(correctly_answered))
print("recall: " + str(recall))
incorrect = number_of_questions-(recall+correctly_answered)
print("incorrect: "+ str(incorrect))
