package cz.brmlab.yodaqa.analysis.passextract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.SearchResult.PF_ClueWeight;
import cz.brmlab.yodaqa.model.SearchResult.PF_AboutClueWeight;
import cz.brmlab.yodaqa.model.SearchResult.Passage;

/**
 * Annotate Passages view "Passage" objects with score based on the associated
 * PassageFeatures.  This particular implementation contains an extremely
 * simple ad hoc score computation with fixed weights. */


public class PassScoreWordEmbeddings extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(PassScoreSimple.class);

	private Probability p=Probability.getInstance();

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	protected class PassScore {
		Passage passage;
		double score;

		public PassScore(Passage passage_, double score_) {
			passage = passage_;
			score = score_;
			//passage.getCoveredText()
		}
	}

		public void process(JCas jcas) throws AnalysisEngineProcessException {
			JCas questionView, passagesView;
			try {
				questionView = jcas.getView("Question");
				passagesView = jcas.getView("Passages");
			} catch (CASException e) {
				throw new AnalysisEngineProcessException(e);
			}

			Collection<Token> wtok=JCasUtil.select(passagesView, Token.class);
			List<String> words = new ArrayList<>();
			Collection<Passage> psg=JCasUtil.select(passagesView, Passage.class);
//			int N=0;
//			for (Token t : wtok) {
//				N++;
//				String word=t.getCoveredText().toLowerCase()
//				if(StringUtils.isAlphanumeric(word)&&!words.contains(word)){
//					words.add(t.getCoveredText().toLowerCase());
//				}
//			}
			p.setidf(psg);

			//JCasUtil.selectCovered(Token.class, passage)
		List<PassScore> passages = new LinkedList<PassScore>();
		for (Passage passage : JCasUtil.select(passagesView, Passage.class)) {
			PassageFV fv = new PassageFV(passage);

			int clueWeight_i = PassageFV.featureIndex(PF_ClueWeight.class);
			int aboutClueWeight_i = PassageFV.featureIndex(PF_AboutClueWeight.class);
			assert(clueWeight_i >= 0 && aboutClueWeight_i >= 0);

//			double score = fv.getValues()[clueWeight_i] + 0.25 * fv.getValues()[aboutClueWeight_i];
//			double score = -fv.getValues()[aboutClueWeight_i];
//			System.out.println("passageText: "+passage.getCoveredText());

			List<String> q=new ArrayList<>(Arrays.asList(questionView.getDocumentText().toLowerCase().split("\\W+")));
			List<String> a=new ArrayList<>(Arrays.asList(passage.getCoveredText().toLowerCase().split("\\W+")));

//			Collection<Token> qtok=JCasUtil.select(passagesView, Token.class);
//			List<String> q = new ArrayList<>();
//			for (Token t : qtok) {
//				q.add(t.getCoveredText());
//			}
//			Collection<Token> atok=JCasUtil.select(passagesView, Token.class);
//			List<String> a = new ArrayList<>();
//			for (Token t : atok) {
//				a.add(t.getCoveredText());
//			}

			double[] res=p.probability(q,a);
			double score = 3.36257418*res[0]+0.00425064*res[2]+0.40970162*fv.getValues()[clueWeight_i];
//			System.out.println("p2="+score);
			// logger.debug(fv.getValues()[clueWeight_i] + " + 0.25 * " + fv.getValues()[aboutClueWeight_i] + " = " + score);
			passages.add(new PassScore(passage, score));
		}

		/* Reindex the touched passages. */
		for (PassScore ps : passages) {
			ps.passage.removeFromIndexes();
			ps.passage.setScore(ps.score);
			ps.passage.addToIndexes();
		}
	}
}
