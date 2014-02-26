package cz.brmlab.yodaqa.io.interactive;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;


/**
 * A collection that talks to the user via stdin/stdout, allowing
 * them to ask questions. */

public class InteractiveCollectionReader extends CollectionReader_ImplBase {
	BufferedReader br;

	private int index;
	private String input;

	@Override
	public void initialize() throws ResourceInitializationException {
		index = -1;

		br = new BufferedReader(new InputStreamReader(System.in));;
	}

	protected void acquireInput() {
		index++;
		if (index == 0) {
			System.out.println("Brmson.YodaQA interactive question answerer");
			System.out.println("(c) 2014  Petr Baudis, standing on the shoulders of giants");
		}
		try {
			System.out.print("brmson.yodaqa> ");
			System.out.flush();
			input = br.readLine();
		} catch (IOException io) {
			io.printStackTrace();
			input = null;
		}
	}

	@Override
	public boolean hasNext() throws CollectionException {
		if (input == null)
			acquireInput();
		return input != null;
	}

	@Override
	public void getNext(CAS aCAS) throws CollectionException {
		if (input == null)
			acquireInput();
		try {
			JCas jcas = aCAS.getJCas();
			jcas.setDocumentText(input);
		} catch (CASException e) {
			throw new CollectionException(e);
		}
		input = null;
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[]{new ProgressImpl(index, -1, Progress.ENTITIES)};
	}

	@Override
	public void close() {
	}
}
