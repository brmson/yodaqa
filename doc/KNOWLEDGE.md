Brmson Knowledge Graph
======================

Right now, Brmson is a question-answering system that does not attempt
to build knowledge, but just directly searches for clues full-text and
synthetizes answers based on the question.  However, long term, it seems quite
useful to switch to a more aggressive abstraction of a knowledge graph (also
known as semantic network).  This needn't actually change any inner workings
of the QA pipeline at first, but it would enable a paradigm switch to deeper
knowledge extraction.  Perhaps we will fork yodaqa to a different project
at that point to underline this.

Knowledge Graph
---------------

The basic data structure we seek to build is a graph where nodes represent
concepts and directed edges represent relationships.  For example, for

	Myrtle Beach is home to the Myrtle Beach Pelicans, a Carolina League baseball team.

the graph would have a structure of

	Myrtle Beach -> home to -> the Myrtle Beach Pelicans
	the Myrtle Beach Pelicans -> is-a -> team/baseball team/Carolina League team/Carolina League baseball team

When given then a question of

	What is the name of the professional baseball team in Myrtle Beach, South Carolina?

we are looking for a path between "professional baseball team" and
"Myrtle Beach, South Carolina". We see that "the Myrtle Beach Pelicans"
is on one such potential path between partial matches, we can use an "edge"
coercion on "home to" vs. "in" and fetch supporting evidence that "the Myrtle
Beach Pelicans" "is-a" professional baseball team and "in" Myrtle Beach,
South Carolina.

The question is how to represent concept hierarchies, like "civil war" vs.
"beginning of civil war".  Also, how to represent multiple names of concepts.
We approach this by having special "non-content" relationships that are
modelled after the Wordnet onthology.  (Then, in the example above, "team"
is not a unique concept; there are many "team" concepts that may be linked
by meronymy and hypernymy, the "team" concept of "the Myrtle Beach Pelicans"
is unique and may have several names - a concept is a Wordnet synset.)

In the future, we may want to include special split-end edges to represent
ordered sequences.

Latent Knowledge Graph
----------------------

A very important thing to realize is that we (at least for now) never have
a complete representation of the knowledge graph!  We are still using
targetted fulltext searches to discover edges and nodes of this graph.
In the end, the result is quite similar to the current pipeline.  So why
bother?

  * This approach gives us a good guideline on what queries and what
    kind of passage filters are worthwhile to get high quality hits.
  * This approach enables caching of commonly referenced concepts, so
    pieces of the graph are learned in time.
  * Deduction over such a graph will be much easier when we get around
    to that.

UIMA CAS Representation
-----------------------

It's still not clear whether to even try to encode the graph information
within UIMA or just keep it completely separate and keep only references
to nodes in the CAS annotations.
