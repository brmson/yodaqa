Generating ntcir xml files
==========================

To generate question analysis output use following scipt:

	data/eval/ntcir/dump_questions.sh question_dir

where question dir contains folders each containing one xml file with questions.
For example question_dir/hokkaido/D844W10.xml, question_dir/chuo-bun/D247W20.xml.

This will generate two files for each input xml file into the data/eval/ntcir/output.
The question analysis file has the same name as the input xml file. The second file describes
the types and its name is the same as an input file with -typedef postfix.


To generate anwser xml files and information retrieval xml files use following script:

	data/eval/ntcir/generate_results.sh question_dir answer_sheet_dir

The question_dr and answer_sheet_dir have same structure as described above.

Note: content of question_dir can be obtained from question-en-phase1-second_stage_examination(2003).zip archive
and content of answer_sheet_dir can be obtained from answer_sheet-en-phase1-second_stage_examination(2003).zip.	