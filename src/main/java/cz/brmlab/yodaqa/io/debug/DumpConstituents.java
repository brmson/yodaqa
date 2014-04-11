package cz.brmlab.yodaqa.io.debug;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Dump a tree of constituents for each sequence.
 */

public class DumpConstituents extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (Constituent c : JCasUtil.select(jcas, ROOT.class)) {
			dumpCTree(c, 0);
		}
	}

	public void dumpCTree(Constituent c, int level) {
		for (int i = 0; i < level; i++)
			System.err.print(" ");
		System.err.println("c " + c.getConstituentType() + " " + c.getSyntacticFunction() + " [" + c.getCoveredText() + "]");

		for (FeatureStructure child : c.getChildren().toArray()) {
			if (child instanceof Token) {
				dumpTLeaf((Token) child, level + 1);
				continue;
			}
			dumpCTree((Constituent) child, level + 1);
		}
	}

	public void dumpTLeaf(Token t, int level) {
		for (int i = 0; i < level; i++)
			System.err.print(" ");
		System.err.println("t " + t.getPos().getPosValue() + " " + t.getLemma().getValue() + " [" + t.getCoveredText() + "]");
	}
}
