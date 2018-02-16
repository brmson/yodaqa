import json
import urllib.request

from FileWriter import FileWriter
from Template import Template

FILE = "diffbot_questions_dataset.tsv"
NUMBER_OF_ENTITIES = 15


def load_templates():
    with open("templates.json") as templates:
        return json.load(templates)


def get_entity(entity_type, index, access_token):
    while True:
        try:
            request = "https://kg.diffbot.com/kg/dql_endpoint?type=query&token=" + access_token + "&from=" + str(
                index) + "&query=type%3A" + entity_type + "&size=1"
            print(request + "\n")
            f = urllib.request.urlopen(request)
            entity_json = json.loads(f.read().decode("utf-8"))
            index += 1
            return entity_json, index
        except Exception as e:
            print(e)
            index += 1


if __name__ == "__main__":
    templates = load_templates()

    file = open(FILE, "w", encoding="utf-8")
    file.write(
        "ID\tQUESTION_TYPE\tQUESTION\tANSWER\tENTITY\tENTITY_TYPE\tENTITY_ID\tRELATION\tANSWER_TYPE\tANSWER_ID\n")
    file.close()

    template = Template()
    file_writer = FileWriter()

    with open("secret.json", "r", encoding="utf-8") as secret_file:
        access_token = json.load(secret_file)["token"]

    with open("templates.json", "r", encoding="utf-8") as in_file:
        entity_objects = json.load(in_file)
        for entity_object in entity_objects:
            entity_type = entity_object["entity_type"]
            attributes = entity_object["attributes"]
            index = 0
            for i in range(NUMBER_OF_ENTITIES):
                entity_json, index = get_entity(entity_type, index, access_token)
                for attribute in attributes:
                    path = attribute["path"]
                    question_templates = attribute["question_templates"]
                    lines = template.generate(question_templates, entity_type, entity_json, path)
                    file_writer.write_to_file(FILE, lines)
