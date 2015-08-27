package cz.brmlab.yodaqa.io.ntcir;

import com.google.common.collect.ArrayListMultimap;
import cz.brmlab.yodaqa.io.ntcir.types.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.io.ntcir.types.*;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * A collection that loads a collection of question and answer patterns
 * from JSON file.
 *
 * The JSON file is an array consisting of json objects containing
 * {qId, qText, answers, author}
 *
 * */
public class NTCIRQuestionReader extends CasCollectionReader_ImplBase {

	class NTCIRQuestion {
		String qId;
		String qText;
		List<String> answers;
		String author;

		public String getqId() {
			return qId;
		}
		public String getqText() {
			return qText;
		}
		public List<String> getAnswers() {
			return answers;
		}
		public String getAuthor() {
			return author;
		}

		public NTCIRQuestion(String qId, String qText, List<String> answers, String author) {
			this.qId = qId;
			this.qText = qText;
			this.answers = answers;
			this.author = author;
		}
	}

	private class OnlyNXML implements FilenameFilter {
		String ext;

		public OnlyNXML(String ext) {
			this.ext = "." + ext;
		}

		public boolean accept(File dir, String name) {
			return name.endsWith(ext);
		}
	}
	/**
	 * Name of optional configuration parameter that contains the language
	 * of questions. This is mandatory as x-unspecified will break e.g. OpenNLP.
	 */
	public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	/**
	 * Name of the Gold standard file.
	 */
	public static final String PARAM_GOLDSTANDARD_FILE = "GOLDSTANDARD_FILE";
	@ConfigurationParameter(name = PARAM_GOLDSTANDARD_FILE, mandatory = true)
	private String goldStandardFileName;

	/**
	 * Name of the question dir.
	 */
	public static final String PARAM_QUESTION_DIR = "QUESTION_DIR";
	@ConfigurationParameter(name = PARAM_QUESTION_DIR, mandatory = true)
	private String questionDir;

	xmlReader xmlreader;
	private int index;

	File testFile[] = null;
	int nCurrFile = 0;
	List<Node> documents = new ArrayList<Node>();
	HashMap<String, Integer> hshAnswers = new HashMap<String, Integer>();
	int nCurrDoc = 0;
	// use to build document text for each cas
	StringBuffer documentText;
	int annoOffset;

	String experimentId;
	String datasetId;
	String experimentInvoker;
	int currentQuestion;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		currentQuestion = 0;
		try {
			File inputDir = new File(questionDir);
			File goldStandardFile = new File(goldStandardFileName);

			if (!goldStandardFile.exists() || !goldStandardFile.isFile()) {
				System.err
						.println("Cannot find gold standard file or it is not a file");
				System.exit(1);
			}
			parseGoldStandards(goldStandardFile);

			if (!inputDir.exists() || !inputDir.isDirectory()) {
				System.err
						.println("Cannot find input directory or it is not a directory");
				System.exit(1);
			}

			testFile = inputDir.listFiles(new OnlyNXML("xml"));
			System.out.println("Total files: " + testFile.length);
			String xmlText = readTestFile();
			xmlText = xmlText.replaceAll("①\\~④", "(1)-(4)");
			xmlText = xmlText.replaceAll("①\\~⑤", "(1)-(5)");
			xmlText = xmlText.replaceAll("①", "(1)");
			xmlText = xmlText.replaceAll("②", "(2)");
			xmlText = xmlText.replaceAll("③", "(3)");
			xmlText = xmlText.replaceAll("④", "(4)");
			xmlText = xmlText.replaceAll("⑤", "(5)");
			parseTestDocument(xmlText);

			experimentId = UUID.randomUUID().toString();
			datasetId = pathToDatasetId(questionDir);
			setExperimentInvoker();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void parseTestDocument(String xmlText) throws Exception {
		DOMParser parser = new DOMParser();
		parser.parse(new InputSource(new StringReader(xmlText)));
		Document document = parser.getDocument();

		NodeList docNodeList = document.getElementsByTagName("question");

		for (int i = 0; i < docNodeList.getLength(); i++) {

			Element questionElement = (Element) docNodeList.item(i);
			String isDoc = questionElement.getAttribute("minimal");

			if (isDoc.equalsIgnoreCase("no")) {
				documents.add(docNodeList.item(i));
			}
		}

	}

	private void parseGoldStandards(File goldFile) throws Exception {
		DOMParser parser = new DOMParser();
		try {
			parser.parse(new InputSource(new FileInputStream(goldFile)));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		Document doc = parser.getDocument();

		NodeList dataList = doc.getElementsByTagName("data");

		for (int i = 0; i < dataList.getLength(); i++) {
			Element dataEle = (Element) dataList.item(i);
			String questionId = dataEle.getElementsByTagName("question_ID")
					.item(0).getTextContent().trim();
			String answer = dataEle.getElementsByTagName("answer").item(0)
					.getTextContent().trim();
			int answerId = Integer.parseInt(answer);

			hshAnswers.put(questionId, answerId);
		}
	}

	private String readTestFile() throws Exception {
		// open input file list iterator
		BufferedReader bfr = null;
		String xmlText = "";
		try {
			xmlText = FileUtils.readFileToString(testFile[nCurrFile]);

			System.out
					.println("Read: " + testFile[nCurrFile].getAbsolutePath());

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bfr != null) {
				bfr.close();
				bfr = null;
			}
		}
		return xmlText;
	}

	private String pathToDatasetId(String inputDirPath) {
		String dataset = inputDirPath;
		if (dataset.startsWith("input/")) {
			dataset = dataset.substring("input/".length());
		}
		if (dataset.endsWith("/")) {
			dataset = dataset.substring(0, dataset.length() - 1);
		}
		return dataset;
	}

	public void setExperimentInvoker() {//What is this for???
		experimentInvoker = System.getProperty("user.name");
		if (experimentInvoker == null || experimentInvoker.isEmpty()) {
			try {
				experimentInvoker = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		index++;
//		NTCIRQuestion j = xmlreader.readNext(this);
		getQuestion(aCAS);
		cz.brmlab.yodaqa.flow.dashboard.Question q = new cz.brmlab.yodaqa.flow.dashboard.Question(this.qId, this.qText);

		QuestionDashboard.getInstance().askQuestion(q);
		QuestionDashboard.getInstance().getQuestionToAnswer();
		System.out.println("GETNEXT:"+qId+" "+qText+" "+ans);
		try {
			//////////////////////XXX
			if(this.ans==null)this.ans=new ArrayList<>();
			JCas jcas = aCAS.getJCas();
			initCas(jcas, /* id */ this.qId,
				/* type */ "factoid",
				/* text */ this.qText,
				/* answerpcre */ this.ans);
			jcas.setDocumentText(this.qText);

			System.err.println(jcas.getDocumentLanguage() + " >>>> " + jcas.getDocumentText());
		} catch (CASException e) {
			throw new CollectionException(e);
		}


	}

	protected void initCas(JCas jcas, String id, String type, String text, List<String> answers) {
		jcas.setDocumentLanguage(language);
		Iterator<String> answerIterator = answers.iterator();
		String answerPattern = "";
		while(answerIterator.hasNext()) {
			answerPattern += answerIterator.next();
			if (answerIterator.hasNext()) {
				answerPattern += "|";
			}
		}
		QuestionInfo qInfo = new QuestionInfo(jcas);
		qInfo.setSource("interactive");
		qInfo.setQuestionId(id);
		qInfo.setQuestionType(type);
		qInfo.setQuestionText(text);
		qInfo.setAnswerPattern(answerPattern);
		qInfo.setProcBeginTime(System.currentTimeMillis());
		qInfo.addToIndexes(jcas);
	}

	private void getQuestion(CAS aCAS) throws CollectionException, IOException {
		documentText = new StringBuffer();
		annoOffset = 0;
		if (nCurrFile < testFile.length && !(nCurrDoc < documents.size())) {
			nCurrDoc = 0;
			nCurrFile++;
			documents = null;
//			getQuestion(aCAS);
		}
		JCas jcas;
		try {
			jcas = aCAS.getJCas();
		} catch (CASException e) {
			throw new CollectionException(e);
		}
		if (documents == null) {
			try {
				String xmlText = readTestFile();
				this.parseTestDocument(xmlText);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		NodeList questionSetNodeList = documents.get(nCurrDoc).getChildNodes();
// The XML is not well designed, so we need to assume that data and
// instruction will appear before the questions so that we could map
// them up
		SetInstruction setInstruction = null;
		Data contextData = null;
		ArrayList<QuestionAnswerSet> qaList = new ArrayList<QuestionAnswerSet>();
		for (int i = 0; i < questionSetNodeList.getLength(); i++) {
			if (questionSetNodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element questionSetNode = (Element) questionSetNodeList.item(i);
				String nodeName = questionSetNode.getNodeName().trim();
				if (nodeName.startsWith("#")) {
					continue;
				}
				if (nodeName.startsWith("data")) {
					Element contextDataElement = questionSetNode;
					contextData = extractContextFromData(jcas,
							contextDataElement);
				}
				if (nodeName.equals("question")) {
					Element qElement = questionSetNode;
					System.out.println("Before annotate");
					QuestionAnswerSet qaSet = annotateQuestion(qElement, jcas);
					System.out.println("After annotate");
					qaList.add(qaSet);
					qaSet.getQuestion().setContextData(contextData);
					qaSet.getQuestion().setSetinstruction(setInstruction);
				}
				if (nodeName.equals("label")) {
					String docId = questionSetNode.getTextContent().trim();
				}
//				if (nodeName.equals("instruction")) {
//					String topInstructionText = questionSetNode
//							.getTextContent().trim();
//// Create SetInstruction
////					setInstruction = new SetInstruction(jcas);
//					String topic = extrctTopicFromSetInstruction(topInstructionText);
//					setInstruction.setTopic(topic);
//					setInstruction.setText(topInstructionText);
//					setInstruction.addToIndexes();
//					String instructionMarker = "[Overall Instruction]\n";
//					documentText.append(instructionMarker + topInstructionText);
//					annoOffset += instructionMarker.length();
//					setInstruction.setBegin(annoOffset);
//					setInstruction.setEnd(annoOffset
//							+ topInstructionText.length());
//					documentText.append("\n\n");
//					annoOffset += topInstructionText.length() + 2;
//				}
			}
		}
//		FSList fsQAList = FSCollectionFactory.createFSList(jcas, qaList);
//		TestDocument testDoc = new TestDocument(jcas);
//		testDoc.setId(String.valueOf(nCurrDoc));
//		testDoc.setInstruction(setInstruction);
//		testDoc.setQAList(fsQAList);
//		testDoc.addToIndexes();
//		jcas.setDocumentText(documentText.toString());
//		jcas.setDocumentLanguage("en");
//		File currentFile = testFile[nCurrFile];
//		setSourceDocumentInformation(jcas, currentFile.toURI().toString(),
//				(int) currentFile.length(), hasNext(), nCurrDoc);
//		connectReferences(jcas);
		nCurrDoc++;
//		annotateExperimentMetadata(jcas);
//		jcas.getDocumentText();

	}

	private void connectReferences(JCas jcas) {
		Map<String, RefTarget> targetMap = new HashMap<String, RefTarget>();
		for (RefTarget target : JCasUtil.select(jcas, RefTarget.class)) {
			targetMap.put(target.getId(), target);
		}
		for (Refs ref : JCasUtil.select(jcas, Refs.class)) {
			ref.setTarget(targetMap.get(ref.getId()));
		}
	}

	private void setSourceDocumentInformation(JCas aJCas, String uri, int size,
											  boolean isLastSegment, int offset) {
		SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(
				aJCas);
		srcDocInfo.setUri(uri);
		srcDocInfo.setOffsetInSource(offset);
		srcDocInfo.setDocumentSize(size);
		srcDocInfo.setLastSegment(isLastSegment);
		srcDocInfo.addToIndexes();
	}

	private String extrctTopicFromSetInstruction(String instruction) {
		String str = "relate to";
		int startIdx = instruction.indexOf(str);
		int endIdx = instruction.indexOf(",", startIdx);
		String topic = "";
		try {
			topic = instruction.substring(startIdx + str.length(), endIdx)
					.trim();
		} catch (Exception e) {
			System.err.println(e);
		}
		return topic;
	}

	private Data extractContextFromData(JCas jcas, Element dataElement) {
		String contextDataMarker = "[Context]\n";
		documentText.append(contextDataMarker);
		annoOffset += contextDataMarker.length();
// Create annotation Data
		Data dataObj = new Data(jcas);
		dataObj.addToIndexes();
		String dataId = dataElement.getAttribute("id");
		dataObj.setId(dataId);
		dataObj.setBegin(annoOffset);
		ArrayListMultimap<String, Annotation> generatedTags = formatTaggedText(
				dataElement, jcas);
		dataObj.setEnd(annoOffset);
		documentText.append("\n\n");
		annoOffset += 2;
		ArrayList<Underlined> uTexts = new ArrayList<Underlined>();
		for (Annotation uText : generatedTags.get("uText")) {
			uTexts.add((Underlined) uText);
		}
		ArrayList<Gaps> gaps = new ArrayList<Gaps>();
		for (Annotation blank : generatedTags.get("blank")) {
			gaps.add((Gaps) blank);
		}
		if (!uTexts.isEmpty()) {
			FSList fsUnderlinedList = FSCollectionFactory.createFSList(jcas,
					uTexts);
			dataObj.setUnderlinedList(fsUnderlinedList);
		}
		if (!gaps.isEmpty()) {
			FSList fsGapList = FSCollectionFactory.createFSList(jcas, gaps);
			dataObj.setGapList(fsGapList);
		}
		dataObj.setText(documentText.substring(dataObj.getBegin(),
				dataObj.getEnd()));
		return dataObj;
	}

	private ArrayListMultimap<String, Annotation> formatTaggedText(
			Element element, JCas jcas) {
		ArrayListMultimap<String, Annotation> annoMap = ArrayListMultimap
				.create();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE) {
				String text = child.getTextContent().trim();
// if empty text, ignored
				if (text.equals("")) {
					continue;
				} else {
// if pure text, append and increment offset
					documentText.append(text + " ");
					annoOffset += text.length() + 1;
				}
			} else if (child.getNodeType() == Node.ELEMENT_NODE) {
// if tag, handle and grab the text, append and increment offset
				Element childElement = (Element) child;
// 1. blank
				if (childElement.getNodeName().equals("blank")) {
					String ii = childElement.getAttribute("id");
					String lbl = childElement.getTextContent().trim();
					if (childElement.getElementsByTagName("label").getLength() > 0) {
						lbl = childElement.getElementsByTagName("label")
								.item(0).getTextContent().trim();
					}
// Create annotation Gaps
					Gaps gap = new Gaps(jcas);
					gap.addToIndexes();
					gap.setId(ii);
					gap.setLabel(lbl);
					gap.addToIndexes();
// Create some gap representation on documentText
					String gapPlaceHolder = "_____";
					documentText.append(" " + gapPlaceHolder + " ");
					annoOffset += 1;
					gap.setBegin(annoOffset);
					annoOffset += gapPlaceHolder.length();
					gap.setEnd(annoOffset);
					annoOffset += 1;
					annoMap.put("blank", gap);
				} else if (childElement.getNodeName().equals("lText")) {
// 2. list
					String ii = childElement.getAttribute("id");
					String lbl = childElement.getElementsByTagName("label")
							.item(0).getTextContent().trim();
// Create annotation ListItem
					ListItem lstItem = new ListItem(jcas);
					lstItem.setId(ii);
					lstItem.setLabel(lbl);
					lstItem.addToIndexes();
					NodeList listContents = childElement.getChildNodes();
					String listTxt = "";
					for (int j = 0; j < listContents.getLength(); j++) {
						Node listContent = listContents.item(j);
						if (listContent.getNodeType() == Node.TEXT_NODE) {
							if (!listContent.getTextContent().trim().equals("")) {
								listTxt = listContent.getTextContent().trim();
							}
						}
					}
					lstItem.setText(listTxt);
					String listPrefix = " ";
					documentText
							.append(listPrefix + lbl + " " + listTxt + "\n");
					annoOffset += (1 + listPrefix.length() + lbl.length());
					lstItem.setBegin(annoOffset);
					annoOffset += listTxt.length();
					lstItem.setEnd(annoOffset);
					annoOffset += 1;
					annoMap.put("lText", lstItem);
				} else if (childElement.getNodeName().equals("ref")) {
// 3. ref
					String refLabel = childElement.getTextContent().trim();
// Create annotation ref
					Refs ref = new Refs(jcas);
					ref.addToIndexes();
					String id = childElement.getAttribute("target");
					ref.setId(id);
					ref.setLabel(refLabel);
					documentText.append(refLabel + " ");
					ref.setBegin(annoOffset);
					annoOffset += refLabel.length();
					ref.setEnd(annoOffset);
					annoOffset += 1;
					if (id.startsWith("B")) {
// target are blanks
						String replacement = childElement.getNextSibling()
								.getTextContent().trim();
						ref.setText(replacement);
					} else if (id.startsWith("L")) {
// target are lists
						String suffix = childElement.getNextSibling()
								.getTextContent().trim();
						ref.setText(suffix);
					} else if (id.startsWith("U")) {
// target are underlined
					} else if (id.startsWith("D")) {
// target are pictures
					}
					annoMap.put("ref", ref);
				} else if (childElement.getNodeName().equals("cNum")) {
// 4. cNum
					ChoiceNumber cNum = new ChoiceNumber(jcas);
					cNum.addToIndexes();
					annoMap.put("cNum", cNum);
					String cNumText = childElement.getTextContent().trim();
					documentText.append(cNumText + " ");
					cNum.setBegin(annoOffset);
					annoOffset += cNumText.length();
					cNum.setEnd(annoOffset);
					annoOffset += 1; // for the space
				} else if (childElement.getNodeName().equals("label")) {
// 5 label (not very useful right now)
					String labelText = childElement.getTextContent().trim();
				} else if (childElement.getNodeName().equals("uText")) {
					String ulabelId = childElement.getAttribute("id");
					String uText = "";
					String label = "";
					for (int j = 0; j < childElement.getChildNodes()
							.getLength(); j++) {
						Node uNodeChild = childElement.getChildNodes().item(j);
						if (uNodeChild.getNodeType() == Node.ELEMENT_NODE) {
							if (uNodeChild.getNodeName().equals("label")) {
								label = uNodeChild.getTextContent();
							} else if (uNodeChild.getNodeName().equals("br")) {
								uText += " ";
							}
						} else if (uNodeChild.getNodeType() == Node.TEXT_NODE) {
							uText += uNodeChild.getTextContent();
						}
					}
					uText = uText.trim();
// Create annotation Underlined
					Underlined context = new Underlined(jcas);
					context.addToIndexes();
					context.setId(ulabelId);
					context.setLabel(label);
					context.setText(uText);
					context.setBegin(annoOffset);
					documentText.append(uText);
					annoOffset += uText.length();
					context.setEnd(annoOffset);
					annoMap.put("uText", context);
					documentText.append(" ");
					annoOffset += 1;
				}
			}
		}
		return annoMap;
	}


	private QuestionAnswerSet annotateQuestion(Element qElement, JCas jcas) {
		String qid = qElement.getAttribute("id");// qElement.getElementsByTagName("id").item(0).getTextContent().trim();
		this.qId = qid;
		System.out.println("QID " + qid);
		String questionLabel = qElement.getElementsByTagName("label").item(0)
				.getTextContent().trim();
		String ansType = qElement.getAttribute("answer_type");
		String knowledgeType = qElement.getAttribute("knowledge_type");
//		String ansColumn = qElement.getElementsByTagName("ansColumn").item(0)
//				.getTextContent().trim();
//		Instruction instr = new Instruction(jcas);
//		instr.addToIndexes();
		ArrayList<QData> qDataList = new ArrayList<QData>();
		ArrayList<AnswerChoice> answerChoiceList = new ArrayList<AnswerChoice>();

		for (int i = 0; i < qElement.getChildNodes().getLength(); i++) {
			Node questionChild = qElement.getChildNodes().item(i);

			if (questionChild.getNodeType() == Node.ELEMENT_NODE) {
				String elementName = questionChild.getNodeName();
				if (elementName.equals("instruction")) {
					// Create annotation Instruction, assuming only one for each
					// question
					Element instrEle = (Element) questionChild;
					String instText = instrEle.getTextContent().trim();
					this.qText = instText;
					String instructionMarker = questionLabel + ": ";
					documentText.append(instructionMarker);
					annoOffset += instructionMarker.length();

					// before format text
					int instrBegin = annoOffset;
//					ArrayListMultimap<String, Annotation> generatedTags = formatTaggedText(
//							instrEle, jcas);
					// after format text
					int instrEnd = annoOffset;

					ArrayList<Refs> refList = new ArrayList<Refs>();
//					for (Annotation instrRefAnno : generatedTags.get("ref")) {
//						Refs instrRef = (Refs) instrRefAnno;
//						refList.add(instrRef);
//					}
//					FSList fsRefList = FSCollectionFactory.createFSList(jcas,
//							refList);
//					instr.setText(instText);
//					instr.setRefList(fsRefList);
//
//					instr.setBegin(instrBegin);
//					instr.setEnd(instrEnd);

					documentText.append("\n\n");
					annoOffset += 2;
				} else if (elementName.equals("data")) {
					// Create annotation QData
					Element qDataEle = (Element) questionChild;
					String type = qDataEle.getAttribute("type").trim();
					if (type == null || !type.equals("text")) {
						continue;
					}
					String dataId = qDataEle.getAttribute("id");
					String dataText = qDataEle.getTextContent().trim();
					System.out.println("TEXT " + dataText);
					this.qText = dataText;
					String qDataMarker = "[Question Data] \n";
					documentText.append(qDataMarker);
					annoOffset += qDataMarker.length();

					int qDataBegin = annoOffset;
//					ArrayListMultimap<String, Annotation> generatedTags = formatTaggedText(
//							qDataEle, jcas);
					int qDataEnd = annoOffset;

					// Create annotation QData
//					QData qData = new QData(jcas, qDataBegin, qDataEnd);
//					qData.addToIndexes();
//					qData.setId(dataId);
//					qData.setText(dataText);

					documentText.append("\n\n");
					annoOffset += 2;

					// Add tags into qData
					ArrayList<Gaps> gaps = new ArrayList<Gaps>();

//					for (Annotation blankAnno : generatedTags.get("blank")) {
//						Gaps instrRef = (Gaps) blankAnno;
//						gaps.add(instrRef);
//					}

					ArrayList<ListItem> listItems = new ArrayList<ListItem>();

//					for (Annotation listAnno : generatedTags.get("lText")) {
//						ListItem listItem = (ListItem) listAnno;
//						listItems.add(listItem);
//					}

					ArrayList<Refs> refs = new ArrayList<Refs>();

//					for (Annotation refAnno : generatedTags.get("ref")) {
//						Refs ref = (Refs) refAnno;
//						refs.add(ref);
//					}

//					if (gaps.size() > 0) {
//						FSList fsGapList = FSCollectionFactory.createFSList(
//								jcas, gaps);
//						qData.setGaps(fsGapList);
//					}
//					if (listItems.size() > 0) {
//						FSList fslstList = FSCollectionFactory.createFSList(
//								jcas, listItems);
//						qData.setListItems(fslstList);
//					}
//					if (refs.size() > 0) {
//						FSList fsRefList = FSCollectionFactory.createFSList(
//								jcas, refs);
//						qData.setRefs(fsRefList);
//					}
//
//					qDataList.add(qData);

				} else if (elementName.equals("choices")) {
					Element choicesEle = (Element) questionChild;
					NodeList choiceList = choicesEle
							.getElementsByTagName("choice");
					for (int j = 0; j < choiceList.getLength(); j++) {
						Element choice = (Element) choiceList.item(j);
						// Create annotation AnswerChoice
						String choiceMarker = "    ";

						documentText.append(choiceMarker);

						annoOffset += choiceMarker.length();

//						AnswerChoice ansChoice = new AnswerChoice(jcas);
//
//						ansChoice.setBegin(annoOffset);
//						ArrayListMultimap<String, Annotation> generatedTags = formatTaggedText(
//								choice, jcas);
//						ansChoice.setEnd(annoOffset);
						this.ans=new ArrayList<>();
						ans.add(choice.getTextContent().trim());
						System.out.println("Choice " + choice.getTextContent().trim());
						documentText.append("\n");
						annoOffset += 1;

//						Annotation cNum = generatedTags.get("cNum").get(0);
//
//						String ansChoiceId = documentText.substring(
//								cNum.getBegin(), cNum.getEnd());
//
//						ansChoice.setId(ansChoiceId);
//						ansChoice.setText(choice.getTextContent().trim());//

						ArrayList<Refs> refList = new ArrayList<Refs>();
						int refIdx = 0;
//						for (Annotation refAnno : generatedTags.get("ref")) {
//							Refs instrRef = (Refs) refAnno;
//							String refText = choice.getElementsByTagName("ref").item(refIdx).getNextSibling().getTextContent();
//							instrRef.setText(refText);
//							refList.add(instrRef);
//							refIdx++;
//						}

//						FSList fsRefList = FSCollectionFactory.createFSList(
//								jcas, refList);
//						ansChoice.setRefList(fsRefList);
//						ansChoice.addToIndexes();

						Integer correctChoice = hshAnswers.get(qid);
						if (correctChoice == null) {
							continue;
						}
//						if (correctChoice == j + 1) {
//							ansChoice.setIsCorrect(true);
//						} else {
//							ansChoice.setIsCorrect(false);
//						}
//						answerChoiceList.add(ansChoice);

					}
				}
			}
		}

		// ////////////////////////////////////////////////

		documentText.append("\n");
		annoOffset += 1;

		// Create annotation Question
		// question is associate with meta data not related to document text.
		Question question = new Question(jcas);
		question.setId(qid);
		question.setKnowledgeType(knowledgeType);
		question.setQuestionType(ansType);
//		question.setInstruction(instr);
		FSList fsQDataList = FSCollectionFactory.createFSList(jcas, qDataList);
		question.setQdataList(fsQDataList);

		QuestionAnswerSet qaSet = new QuestionAnswerSet(jcas);
		qaSet.setQuestion(question);
		FSList fsAnswerChoiceList = FSCollectionFactory.createFSList(jcas,
				answerChoiceList);
		qaSet.setAnswerChoiceList(fsAnswerChoiceList);
//		this.qId=qid;
//		this.qText=question.getCoveredText();
//		this.ans=new ArrayList<>();
//		ans.add(answerChoiceList.get(0).getText());
		return qaSet;
	}

	private String qId;
	private String qText;
	private List<String> ans;
	private String author;

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		if (nCurrFile < testFile.length && nCurrDoc < documents.size()) {
			return true;
		}
		return false;
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[]{new ProgressImpl(index, -1, Progress.ENTITIES)};
	}

}
