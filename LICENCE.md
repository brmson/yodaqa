Governing Licences
==================

This project and all contributions are per se dual-licenced under the:

  * ASLv2 licence (LICENCE-ASL2.txt)

  * GPLv3 (LICENCE-GPL3.txt) or any later version of the GPL licence

You can choose, at your option, to follow the terms of either or produce
derivative works licenced under the terms of either, however you are
encourged to dual-licence your derivative works in the same way so that
code can be merged back to the original project unhampered.


Rationale
---------

The primary reason for dual-licencing is the fact that Stanford CoreNLP
(and possibly some other tools used in the future) is licenced under
the GPLv2+ licence.  Therefore, a binary distribution of this project that
includes calls to (third-party, source not included in this project)
CoreNLP-based annotators (the default setup) may need to be licenced
under GPLv3!

However, the CoreNLP components are not essential and the system continues
to work (albeit with reduced performance) when they are simply removed (or
could feasibly continue to work with small modifications).  At the same time,
ASLv2 is the common licence in the Java NLP ecosystem.  Therefore, we aim to
also licence our work under the terms of ASLv2 and we opted for dual-licencing.
