package cz.brmlab.yodaqa.analysis.passage.biotagger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import approxlib.tree.LblTree;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/** Interface between UIMA CAS and approxlib's LblTree.  Basically builds
 * up LblTree from a given CAS (or its sub-span given by an annotation).
 *
 * We build a string descriptor of the parse tree, which we then pass to
 * LblTree's fromString() method.
 *
 * Use the static methods; do not instantiate the class on your own. */

public class LblTreeCASFactory {
	final Logger logger = LoggerFactory.getLogger(LblTreeCASFactory.class);

	protected JCas jcas;

	/* Internal dependency tree representation. */
	protected Map<Token, Integer> tokenToWordIdx = new HashMap<>();
	protected Multimap<Token, Dependency> tokenChildDeps = HashMultimap.create();
	protected Set<Token> roots = new HashSet<>();
	protected String sentence;

	/** Instantiate the LblTree factory for a single run. */
	public LblTreeCASFactory(JCas jcas) {
		this.jcas = jcas;
	}

	/** Build internal tree representation from a sequence of tokens. */
	public void buildTree(Collection<Token> tokens, Collection<Dependency> deps) {
		int i = 0;
		StringBuilder sb = new StringBuilder();
		for (Token t : tokens) {
			tokenToWordIdx.put(t, i);
			sb.append(t.getCoveredText());
			sb.append(" ");
			i++;
		}
		this.sentence = sb.toString();

		for (Dependency d : deps) {
			tokenChildDeps.put(d.getGovernor(), d);
		}

		roots.addAll(tokenChildDeps.keySet());
		for (Dependency d : deps) {
			roots.remove(d.getDependent());
		}
	}

	public LblTree toLblTree() {
		if (roots.isEmpty()) {
			logger.debug("{} --- no dependency tree roots", this.sentence);
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("-1:{"); // treeID
		sb.append("-1:{"); // "virtual" root (we can have multiple true roots)
		for (Token root : roots) {
			sb.append("root");
			try {
				appendTokenToSB(sb, root, "root");
			} catch (StackOverflowError e) {
				// sometimes we get lost in recursion of appendTokenToSB(); FIXME look into why
				logger.debug("{} --- stack overflow, sb {}", this.sentence, sb.toString());
				e.printStackTrace();
				return null;
			}
		}
		sb.append("}}");
		logger.debug("{} --- {}", this.sentence, sb.toString());
		return LblTree.fromString(sb.toString());
	}

	protected void appendTokenToSB(StringBuilder sb, Token t, String dep) {
		sb.append("{");
		sb.append(tokenToWordIdx.get(t));
		sb.append(":");
		sb.append(t.getCoveredText().replaceAll("/", "_").replaceAll("\\{", "_").replaceAll("\\}", "_").replaceAll(":", "_"));
		sb.append("/");
		sb.append(t.getLemma().getValue().replaceAll("/", "_").replaceAll("\\{", "_").replaceAll("\\}", "_").replaceAll(":", "_"));
		sb.append("/");
		sb.append(t.getPos().getPosValue());
		sb.append("/");
		sb.append(dep);
		for (Dependency d : tokenChildDeps.get(t)) {
			if (d.getDependent() == t) {
				/* <<This view is reflected in numerous publications and also artistic works on the theme of the Chernobyl accident that appeared immediately after the accident , and for a long time remained dominant in the public consciousness and in popular publications .>>
				 * has some weird parse tree where
				 * conj-and[works] has this loop. */
				logger.debug("{} --- skipping self-reference loop on {} ({}-{})",
						sentence, d.getDependent().getCoveredText(),
						dep, d.getDependencyType());
				continue;
			}
			sb.append("{");
			sb.append(d.getDependencyType());
			appendTokenToSB(sb, d.getDependent(), d.getDependencyType());
			sb.append("}");
		}
		sb.append("}");
	}

	public static LblTree casToTree(JCas jcas) {
		LblTreeCASFactory f = new LblTreeCASFactory(jcas);
		f.buildTree(JCasUtil.select(jcas, Token.class), JCasUtil.select(jcas, Dependency.class));
		return f.toLblTree();
	}

	public static LblTree spanToTree(JCas jcas, Annotation span) {
		LblTreeCASFactory f = new LblTreeCASFactory(jcas);
		f.buildTree(JCasUtil.selectCovered(Token.class, span), JCasUtil.selectCovered(Dependency.class, span));
		return f.toLblTree();
	}

}
