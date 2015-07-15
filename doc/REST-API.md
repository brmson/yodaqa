YodaQA REST API
===============

Question asking
---------------

To start answering process use POST request method to `/q` with text attribute set to asked question. Return value is ID of question.


Answer retrieving
-----------------

To retrieve answers use GET request method in format `/q/<id>`, where `id` is id returned during questioning. Answers is returned in JSON.


Answered questions
------------------

To retrieve last six questions use GET request method in format `/q/?answered`. Questions are returned in JSON. Returned JSON contains array of questions.


Question in progress
--------------------

To retrieve question in progress use GET request method in format `/q/?inProgress`. Waiting questions are returned in JSON.


Questions in queue
------------------

To retrieve questions waiting in queue use GET request method in format `/q/?toAnswer`. Waiting questions are returned in JSON.


Structure of answer's JSON object
---------------------------------

Structure of answers JSON object is:

* id - question's id
* text - question's text
* summary - object with question's summary with structure:
  * lats - [Array] summary of answer types (eq. For question "Who created Microsoft?" lats will be "Person", because user is asking for person)
  * concepts - [Array] array of objects with concepts. Concept is page, which contains answer with high probability.  Concept object has structure:
    * title - title of concept page
    * pageID - id of Wikipedia concept page

* sources - [Map] map of sources. Source is object representing web page, which contains answer. Source object has structure:
  * URL - (optional) URL of source
  * origin - contains information, where question was founded. Possible values are: 
    * "title-in-clue" - question was founded in page title 
    * "fulltext" - question was founded in full text search on page
    * "structured" - question was founded in structured database
  * pageId - (optional) id of Wikipedia page
  * isConcept - (optional) if page is concept or not
  * type - type of source (eg. “enwiki”, "structured")
  * title - title of origin’s page
  * sourceID - source's id
  * state - ???

* answers – [Array] array of answers. Answer is represented by object with structure:
  * text - answer’s text
  * confidence – answer’s confidence expressed by number from interval of <0, 1>
  * ID – answer’s id
  * snippetIDs – [Array] IDs of snippets assigned to answer

* snippets - [Map] map of snippets. Snippets is object containing addition explanation of answer. It has structure:
  * passageText - (optional) Text in which answer was founded
  * propertyLabel - (optional) Label of snippet
  * snippetID - snippet's id
  * sourceID - snipet's source

* finished – if question has been finished already
* gen_sources – number of generated sources (but only ??? are present in sources array)
* gen_answers – number of generated answers (but only 25 best are present in answers array)
