package cz.brmlab.yodaqa;

import java.io.File;
import java.lang.Thread;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;

import cz.brmlab.yodaqa.io.interactive.InteractiveQuestionReader;


public class YodaQAApp {
	public static void main(String[] args) {
		try {
			XMLInputSource in = new XMLInputSource(YodaQAApp.class.getResource("/cz/brmlab/yodaqa/pipeline/YodaQA.xml"));
			ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

			AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier);
			CAS cas = ae.newCAS();

			CollectionReader_ImplBase reader = new InteractiveQuestionReader();
			reader.initialize();
			while (reader.hasNext()) {
				reader.getNext(cas);

				ae.process(cas);

				cas.reset();
			}
		} catch (Exception e) {
			System.err.println("YodaQA exception: " + e);
			Thread.currentThread().dumpStack();
		}
	}
}
