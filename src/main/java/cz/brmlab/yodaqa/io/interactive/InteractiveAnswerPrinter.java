package cz.brmlab.yodaqa.io.interactive;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.FinalAnswer.Answer;

/**
 * A trivial consumer that will extract the final answer and print it
 * on the standard output for the user to "officially" see.
 *
 * Pair this with InteractiveCollectionReader.
 */

public class InteractiveAnswerPrinter extends JCasConsumer_ImplBase {

	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		FSIndex idx = jcas.getJFSIndexRepository().getIndex("SortedAnswers");
		FSIterator answers = idx.iterator();
		if (answers.hasNext()) {
			int i = 1;
			while (answers.hasNext()) {
				Answer answer = (Answer) answers.next();
				System.out.println((i++) + ". " + answer.getText() + " (conf. " + answer.getConfidence() + ")");
			}
		} else {
			System.out.println("No answer found.");
		}
	}
}
