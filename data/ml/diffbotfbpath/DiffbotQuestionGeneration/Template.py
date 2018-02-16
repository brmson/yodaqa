import re


class Template:
    def generate(self, template, entity_type, entity_json, path):
        try:
            entity_json = entity_json["data"][0]
            values = self.get_entity_and_value(entity_json, path)
            return self.make_lines(entity_type, values, template, path)
        except Exception as e:
            print(e)
        return []

    def get_entity_and_value(self, entity_json, path):
        name = entity_json["name"]
        id = entity_json["id"]
        value = str(self.get_value_from_path(path, entity_json)).replace("\n", "").replace("\r", "")

        if len(path.split("|")) > 1:
            path = "|".join(path.split("|")[:-1])
            value_type = self.get_answer_type(path, entity_json)
            value_id = self.get_answer_id(path, entity_json)
        else:
            value_type = ""
            value_id = ""

        return (name, id, value, value_type, value_id)

    def get_value_from_path(self, path, entity_json):
        splited_path = path.split("|")
        json = entity_json
        for i, path in enumerate(splited_path):
            json = json[path]

            if type(json) is list:
                rest_of_path = "|".join(splited_path[i + 1:])
                if rest_of_path == "":
                    return "|".join(json)
                to_return = []
                for el in json:
                    try:
                        to_return += [self.get_value_from_path(rest_of_path, el)]
                    except Exception as e:
                        print(e)
                json = "|".join(to_return)
                return json

        return json

    def get_answer_id(self, path, entity_json):
        splited_path = path.split("|")
        json = entity_json
        for i, path in enumerate(splited_path):
            json = json[path]
            if type(json) is list:
                rest_of_path = "|".join(splited_path[i + 1:])
                to_return = []
                for el in json:
                    try:
                        to_return += [self.get_answer_id(rest_of_path, el)]
                    except Exception as e:
                        print(e)
                json = "|".join(to_return)
                return json

        if "id" in json:
            return json["id"]
        else:
            return ""

    def get_answer_type(self, path, entity_json):
        splited_path = path.split("|")
        json = entity_json
        for i, path in enumerate(splited_path):
            json = json[path]

            if type(json) is list:
                rest_of_path = "|".join(splited_path[i + 1:])
                to_return = []
                for el in json:
                    try:
                        to_return += [self.get_answer_type(rest_of_path, el)]
                    except Exception as e:
                        print(e)
                json = "|".join(to_return)
                return json

        if "type" in json:
            return json["type"]
        else:
            return ""

    def make_lines(self, entity_type, entity_and_value, templates, path):
        lines = []
        for template in templates:
            entity = entity_and_value[0]
            entity_id = entity_and_value[1]
            value = entity_and_value[2]
            value_type = entity_and_value[3]
            value_id = entity_and_value[4]
            question = template.replace("#" + entity_type.upper(), entity)
            lines.append([question, entity, entity_type, entity_id, path, value, value_type, value_id])
        return lines
