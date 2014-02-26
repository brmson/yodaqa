YodaQA
======

YodaQA is a Question Answering system built on top of the Apache UIMA
framework.  It stands for "Yet anOther Deep Answering pipeline" and it
is inspired by the DeepQA (IBM Watson) papers and the OpenQA framework.
Its goals are practicality, clean design and maximum simplicity - we
believe that other nice features like usability for research will flow
naturally from this.

YodaQA is developed as part of the Brmson platform; many of its components
are or will be based on the work of the good scientists at CMU (bits of
OpenQA, the Ephyra project).  We follow a significantly different (more
flexible, we believe) architecture compared to OpenQA, though.

The current version is a work-in-progress snapshot that does nothing
useful yet.

## Installation Instructions

Quick instructions for setting up, building and running (focused on Debian Wheezy):
  * We assume that you cloned YodaQA and are now in the directory that contains this README.
  * ``sudo apt-get install default-jdk maven uima-utils``
  * ``mvn verify``
  * ``mvn -q exec:java``

## Design Considerations

See the [High Level Design Notes](doc/HIGHLEVEL.md) document for
a brief description of YodaQA's design approach.

### Package Organization

We live in the cz.brmlab.yodaqa namespace. The rest is T.B.D. yet.
