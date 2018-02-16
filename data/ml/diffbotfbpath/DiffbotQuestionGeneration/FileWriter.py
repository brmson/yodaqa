import re


class FileWriter:
    QUESTION = 0
    ENTITY = 1
    ENTITY_TYPE = 2
    ENTITY_ID = 3
    RELATION = 4
    ANSWER = 5
    ANSWER_TYPE = 6
    ANSWER_ID = 7

    id = 0

    def write_to_file(self, file, lines):
        with open(file, "a", encoding="utf-8") as out_file:
            for line in lines:
                if line == []:
                    continue
                out_file.write(str(FileWriter.id) + "\t")
                out_file.write("factoid\t")
                out_file.write(str(line[self.QUESTION]) + "\t")
                out_file.write(self.escape(str(line[self.ANSWER])) + "\t")
                out_file.write(str(line[self.ENTITY]) + "\t")
                out_file.write(str(line[self.ENTITY_TYPE]) + "\t")
                out_file.write(str(line[self.ENTITY_ID]) + "\t")
                out_file.write(str(line[self.RELATION]) + "\t")
                out_file.write(str(line[self.ANSWER_TYPE]) + "\t")
                out_file.write(str(line[self.ANSWER_ID]) + "\t")
                out_file.write("\n")
                FileWriter.id += 1

    def escape(self, text):
        escaped_texts = []
        texts = text.split("|")
        for one_text in texts:
            escaped_text = re.escape(one_text)
            escaped_text = escaped_text.replace("\ "," ")
            escaped_texts.append(escaped_text)

        return "|".join(escaped_texts)
