package cz.brmlab.yodaqa.analysis.question;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ADVMOD;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DEP;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DET;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.Focus;

/**
 * If Focus in Question CAS is "name", add extra focus for the actual object.
 * If we see "What is the name of a volcano...?" or "What is the name of X's
 * wife?", the focus/LAT of "name" is not all that useful, so also add
 * _another_ focus for the "volcano" or "wife".  We simply recurse through
 * dependencies governed by "name" until we get to something else than a
 * preposition. */

public class FocusNameProxy extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(FocusGenerator.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (ROOT sentence : JCasUtil.select(jcas, ROOT.class)) {
			processSentence(jcas, sentence);
		}
	}

	public void processSentence(JCas jcas, Constituent sentence) throws AnalysisEngineProcessException {
		for (Focus f : JCasUtil.selectCovered(Focus.class, sentence)) {
			Token ftok = f.getToken();
			if (!ftok.getLemma().getValue().equals("name"))
				continue;
			processAllGoverned(jcas, sentence, ftok);
		}
	}

	public void processAllGoverned(JCas jcas, Constituent sentence, Token gov) throws AnalysisEngineProcessException {
		for (Dependency d : JCasUtil.selectCovered(Dependency.class, sentence)) {
			if (d.getGovernor() != gov)
				continue;
			if (d.getDependencyType().equals("prep")) { // - of -
				processAllGoverned(jcas, sentence, d.getDependent());
			} else if (d.getDependencyType().equals("det")) { // the -
				// ignore
			} else {
				processProxied(jcas, sentence, d.getDependent());
			}
		}
	}

	public void processProxied(JCas jcas, Constituent sentence, Token proxied) throws AnalysisEngineProcessException {
		Focus f = new Focus(jcas);
		f.setBegin(proxied.getBegin());
		f.setEnd(proxied.getEnd());
		f.setBase(proxied);
		f.setToken(proxied);
		f.addToIndexes();
	}
}
