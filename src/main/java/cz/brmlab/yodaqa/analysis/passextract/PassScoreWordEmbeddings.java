package cz.brmlab.yodaqa.analysis.passextract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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

			Collection<Passage> psg=JCasUtil.select(passagesView, Passage.class);

			p.setidf(psg);

		List<PassScore> passages = new LinkedList<PassScore>();
		for (Passage passage : JCasUtil.select(passagesView, Passage.class)) {
			PassageFV fv = new PassageFV(passage);

			int clueWeight_i = PassageFV.featureIndex(PF_ClueWeight.class);
			assert(clueWeight_i >= 0);
			List<String> q=new ArrayList<>(Arrays.asList(questionView.getDocumentText().toLowerCase().split("\\W+")));
			List<String> a=new ArrayList<>(Arrays.asList(passage.getCoveredText().toLowerCase().split("\\W+")));

			double[] res=p.probability(q,a);

//			double score = 2.26216399*res[0]+0.49076233*fv.getValues()[clueWeight_i];
			double score = 3.32822695*res[0]+0.40156513*fv.getValues()[clueWeight_i]-3.96791205;
//			System.out.println("TEXT:"+passage.getCoveredText());
//			for(int i=0;i<fv.getValues().length;i++){
//				System.out.println("fv.getValues "+i+":"+fv.getValues()[i]);
//			}
//			int numClues = JCasUtil.select(questionView, Clue.class).size();
//			System.out.println("numClues="+numClues);
//			if(Math.random()>0.99)System.exit(0);
			score=1/(1+Math.exp(-score));

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
