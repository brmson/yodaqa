Text entailment in MovieQA
=====

Text entailment in closed-domain QA should in theory have several
advantages over open-domain QA,both performance-wise and during analysis.
For example, we can anticipate certain kinds of questions and optimize accordingly.
Some questions also become an easy case of structured database search. 

The first step would be to consistently recognize closed (aka yes/no) questions.
A closed question should (in fact, must) include one of the verbs “be”,
“do”, “have” or a modal verb. Since modal verbs include verbs such as
 “can”, “could”, “may”, “might”, “should”, “must”, “would” or “ought to” and 
questions containing these verbs (except “can” in some cases) are usually 
difficult to answer, I will disregard them.

Examples include:
* Did X play in Y?
* Was X in the movie Y? (we are asking for the same information using a different sentence)
* Did X play with Y in a movie? (these questions might not yet be answerable in our current state)
* Has X received an academy award?
* Is X still alive?
* Can X kill a bear with his bare hands?

Afterwards, we must change our answer generation. Usually we output
several question candidates during the search and the final answers
with the scoring at the end. In the case of closed questions, we would 
ideally only output “yes” or “no”, with some information about the reasoning, 
such as the source. 

This really depends on the accuracy of our question analysis, as the wrong 
assumption would generate useless answers (e.g. Who was the first US president? - “Yes.”).
But I will assume that we always recognize the correct 
type of question for any grammatically correct input.

Now we will try to (very) broadly show the solutions for the given question categories.
 We consider only freebase search, as the fulltext search would have different properties.
* Questions about properties: Has X received an academy award – 
	We would have to look for the “awards” property of X.
* Questions about 2 entity relations: Did X play in/direct/wrote screenplay for Y – 
	We will have to search a path from X to Y with the same/similar label as the verb. 
For example when we ask “Did Keanu Reeves direct the Matrix?”, the answer should be no.
* Questions about 3 entity relations: Did X play with Y in a movie? - 
	Questions of this kind have more 'concepts' which we currently do not answer, 
	but it might still be desirable to do so. Considering the graph structure 
	of the database, it is definitely not trivial.
	
It would be probably easier to answer this using fulltext search. In 
this very simple case we could first find the list of movies where X starred. 
Then in each of those movie entries, we would look for Y. This is both slow 
(negating any performance boost from using structured database search in closed domain),
hard to parallelize and just not elegant.

At last, we should also reconsider the certainty scoring (what does it mean 
when we're 60% certain about “yes”?) and the sources. 
During normal search, we have multiple text passages and properties for any given question.
 In our simplified model, it would probably be only one source. 
