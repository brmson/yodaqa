import sys

# This script parses the tsv from our google doc to a json format
# It creates both train and test datasets by juggling the output writer reference

# save the json output as output.json 

argv = sys.argv
input_filename = argv[1]
output_filename = argv[2]
train = file(output_filename + "_train.json", 'w')
test = file(output_filename + "_test.json", 'w')

train.write('[\r\n')
test.write('[\r\n')
jsfile = train
with open(input_filename,'r') as f:
    reader=f.readlines()
    i = 1
    last_train = len(reader)
    last_test = 0
    while (last_test + 4) < last_train:
        last_test += 4
    for line in reader:
        if (i < 6): #skip the first 5 lines containing the readme and title
           i+=1
           continue
        if (i%4 == 0):
			jsfile = test
        else:
            jsfile = train
        words = line.split("\t")
        jsfile.write('{')
        jsfile.write('"qID\": \"' + str(i) + '\", ')
        jsfile.write('"qText\": \"' + words[1] + '\", ')
        jsfile.write("\"answers\": [")
        jsfile.write("\""+words[2]+"\"")
        j = 3
        
        while (words[j] != "" and j<len(words)-1): #iterate through answers
            jsfile.write(", \""+words[j]+"\"")
            j+=1
        jsfile.write("], ")
        jsfile.write("\"author\": "+"\""+words[0]+"\"")
        jsfile.write('}')
       
        if (i == last_test or i == last_train):
            jsfile.write('\n')
            i += 1
            continue
        jsfile.write(',')
        jsfile.write('\n')
        i += 1
    train.write(']')
    test.write(']')
train.close()
test.close()
