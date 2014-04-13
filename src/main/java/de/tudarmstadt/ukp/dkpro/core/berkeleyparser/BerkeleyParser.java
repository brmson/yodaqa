/**
 * Copyright 2007-2014
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tudarmstadt.ukp.dkpro.core.berkeleyparser;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.toText;
import static org.apache.uima.util.Level.INFO;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.SingletonTagset;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.resources.CasConfigurableProviderBase;
import de.tudarmstadt.ukp.dkpro.core.api.resources.MappingProvider;
import de.tudarmstadt.ukp.dkpro.core.api.resources.ModelProviderBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.PennTree;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import edu.berkeley.nlp.PCFGLA.CoarseToFineMaxRuleParser;
import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.Lexicon;
import edu.berkeley.nlp.PCFGLA.Parser;
import edu.berkeley.nlp.PCFGLA.ParserData;
import edu.berkeley.nlp.PCFGLA.TreeAnnotations;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;

/**
 * Berkeley Parser annotator. Requires {@link Sentence}s to be annotated before.
 *
 * @author Richard Eckart de Castilho
 */
@OperationalProperties(multipleDeploymentAllowed = false)
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence" }, outputs = {
        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent",
        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.PennTree" })
public class BerkeleyParser
    extends JCasAnnotator_ImplBase
{
    /**
     * Use this language instead of the language set in the CAS to locate the model.
     */
    public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;
    @ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = false)
    protected String language;

    /**
     * Override the default variant used to locate the model.
     */
    public static final String PARAM_VARIANT = ComponentParameters.PARAM_VARIANT;
    @ConfigurationParameter(name = PARAM_VARIANT, mandatory = false)
    protected String variant;

    /**
     * Load the model from this location instead of locating the model automatically.
     */
    public static final String PARAM_MODEL_LOCATION = ComponentParameters.PARAM_MODEL_LOCATION;
    @ConfigurationParameter(name = PARAM_MODEL_LOCATION, mandatory = false)
    protected String modelLocation;

    /**
     * Load the part-of-speech tag to UIMA type mapping from this location instead of locating the
     * mapping automatically.
     */
    public static final String PARAM_POS_MAPPING_LOCATION = ComponentParameters.PARAM_POS_MAPPING_LOCATION;
    @ConfigurationParameter(name = PARAM_POS_MAPPING_LOCATION, mandatory = false)
    protected String posMappingLocation;

    /**
     * Use the {@link String#intern()} method on tags. This is usually a good idea to avoid spaming
     * the heap with thousands of strings representing only a few different tags.
     *
     * Default: {@code true}
     */
    public static final String PARAM_INTERN_TAGS = ComponentParameters.PARAM_INTERN_TAGS;
    @ConfigurationParameter(name = PARAM_INTERN_TAGS, mandatory = false, defaultValue = "true")
    private boolean internTags;

    /**
     * Log the tag set(s) when a model is loaded.
     *
     * Default: {@code false}
     */
    public static final String PARAM_PRINT_TAGSET = ComponentParameters.PARAM_PRINT_TAGSET;
    @ConfigurationParameter(name = PARAM_PRINT_TAGSET, mandatory = true, defaultValue = "false")
    protected boolean printTagSet;

    /**
     * Sets whether to create or not to create POS tags. The creation of constituent tags must be
     * turned on for this to work.
     *
     * Default: {@code true}
     */
    public static final String PARAM_WRITE_POS = ComponentParameters.PARAM_WRITE_POS;
    @ConfigurationParameter(name = PARAM_WRITE_POS, mandatory = true, defaultValue = "true")
    private boolean createPosTags;

    /**
     * If this parameter is set to true, each sentence is annotated with a PennTree-Annotation,
     * containing the whole parse tree in Penn Treebank style format.
     *
     * Default: {@code false}
     */
    public static final String PARAM_WRITE_PENN_TREE = ComponentParameters.PARAM_WRITE_PENN_TREE;
    @ConfigurationParameter(name = PARAM_WRITE_PENN_TREE, mandatory = true, defaultValue = "false")
    private boolean createPennTreeString;

    /**
     * Compute Viterbi derivation instead of max-rule tree.
     *
     * Default: {@code false} (max-rule)
     */
    public static final String PARAM_VITERBI = "viterbi";
    @ConfigurationParameter(name = PARAM_VITERBI, mandatory = true, defaultValue = "false")
    private boolean viterbi;

    /**
     * Output sub-categories (only for binarized Viterbi trees).
     *
     * Default: {@code false}
     */
    public static final String PARAM_SUBSTATES = "substates";
    @ConfigurationParameter(name = PARAM_SUBSTATES, mandatory = true, defaultValue = "false")
    private boolean substates;

    /**
     * Output inside scores (only for binarized viterbi trees).
     *
     * Default: {@code false}
     */
    public static final String PARAM_SCORES = "scores";
    @ConfigurationParameter(name = PARAM_SCORES, mandatory = true, defaultValue = "false")
    private boolean scores;

    /**
     * Set thresholds for accuracy.
     *
     * Default: {@code false} (set thresholds for efficiency)
     */
    public static final String PARAM_ACCURATE = "accurate";
    @ConfigurationParameter(name = PARAM_ACCURATE, mandatory = true, defaultValue = "false")
    private boolean accurate;

    /**
     * Use variational rule score approximation instead of max-rule
     *
     * Default: {@code false}
     */
    public static final String PARAM_VARIATIONAL = "variational";
    @ConfigurationParameter(name = PARAM_VARIATIONAL, mandatory = true, defaultValue = "false")
    private boolean variational;

    /**
     * Retain predicted function labels. Model must have been trained with function labels.
     *
     * Default: {@code false}
     */
    public static final String PARAM_KEEP_FUNCTION_LABELS = "keepFunctionLabels";
    @ConfigurationParameter(name = PARAM_KEEP_FUNCTION_LABELS, mandatory = true, defaultValue = "false")
    private boolean keepFunctionLabels;

    /**
     * Output binarized trees.
     *
     * Default: {@code false}
     */
    public static final String PARAM_BINARIZE = "binarize";
    @ConfigurationParameter(name = PARAM_BINARIZE, mandatory = true, defaultValue = "false")
    private boolean binarize;

    private CasConfigurableProviderBase<Parser> modelProvider;
    private MappingProvider posMappingProvider;
    private MappingProvider constituentMappingProvider;

    @Override
    public void initialize(UimaContext aContext)
        throws ResourceInitializationException
    {
        super.initialize(aContext);

        modelProvider = new BerkeleyParserModelProvider();

        posMappingProvider = new MappingProvider();
        posMappingProvider.setDefault(MappingProvider.LOCATION,
                "classpath:/de/tudarmstadt/ukp/dkpro/"
                        + "core/api/lexmorph/tagset/${language}-${pos.tagset}-pos.map");
        posMappingProvider.setDefault(MappingProvider.BASE_TYPE, POS.class.getName());
        posMappingProvider.setDefault("pos.tagset", "default");
        posMappingProvider.setOverride(MappingProvider.LOCATION, posMappingLocation);
        posMappingProvider.setOverride(MappingProvider.LANGUAGE, language);
        posMappingProvider.addImport("pos.tagset", modelProvider);

        constituentMappingProvider = new MappingProvider();
        constituentMappingProvider
                .setDefault(
                        MappingProvider.LOCATION,
                        "classpath:/de/tudarmstadt/ukp/dkpro/"
                                + "core/api/syntax/tagset/${language}-${constituency.tagset}-constituency.map");
        constituentMappingProvider.setDefault(MappingProvider.BASE_TYPE,
                Constituent.class.getName());
        constituentMappingProvider.setDefault("constituency.tagset", "default");
        constituentMappingProvider.setOverride(MappingProvider.LOCATION, posMappingLocation);
        constituentMappingProvider.setOverride(MappingProvider.LANGUAGE, language);
        constituentMappingProvider.addImport("constituency.tagset", modelProvider);
    }

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        CAS cas = aJCas.getCas();

        modelProvider.configure(cas);
        posMappingProvider.configure(cas);
        constituentMappingProvider.configure(cas);

        for (Sentence sentence : select(aJCas, Sentence.class)) {
		System.err.println("... " + sentence.getCoveredText());
            List<Token> tokens = selectCovered(aJCas, Token.class, sentence);
            List<String> tokenText = toText(tokens);

            Tree<String> parseOutput = modelProvider.getResource().getBestParse(tokenText);
            
            // Check if the sentence could be parsed or not
            if (parseOutput.getChildren().isEmpty()) {
                getLogger().warn("Unable to parse sentence: [" + sentence.getCoveredText() + "]");
                continue;
            }
            
            if (!binarize) {
                parseOutput = TreeAnnotations.unAnnotateTree(parseOutput, keepFunctionLabels);
            }

            createConstituentAnnotationFromTree(aJCas, parseOutput, null, tokens, new MutableInt(0));

            if (createPennTreeString) {
                PennTree pTree = new PennTree(aJCas, sentence.getBegin(), sentence.getEnd());
                pTree.setPennTree(parseOutput.toString());
                pTree.addToIndexes();
            }
        }
    }

    /**
     * Creates linked constituent annotations + POS annotations
     *
     * @param aNode
     *            the source tree
     * @return the child-structure (needed for recursive call only)
     */
    private Annotation createConstituentAnnotationFromTree(JCas aJCas, Tree<String> aNode,
            Annotation aParentFS, List<Token> aTokens, MutableInt aIndex)
    {
        // If the node is a word-level constituent node (== POS):
        // create parent link on token and (if not turned off) create POS tag
        if (aNode.isPreTerminal()) {
            Token token = aTokens.get(aIndex.intValue());

            // link token to its parent constituent
            if (aParentFS != null) {
                token.setParent(aParentFS);
            }

            // only add POS to index if we want POS-tagging
            if (createPosTags) {
                String typeName = aNode.getLabel();
                Type posTag = posMappingProvider.getTagType(typeName);
                POS posAnno = (POS) aJCas.getCas().createAnnotation(posTag, token.getBegin(),
                        token.getEnd());
                posAnno.setPosValue(internTags ? typeName.intern() : typeName);
                posAnno.addToIndexes();
                token.setPos(posAnno);
            }

            aIndex.add(1);

            return token;
        }
        // Check if node is a constituent node on sentence or phrase-level
        else {
            String typeName = aNode.getLabel();

            // create the necessary objects and methods
            Type constType = constituentMappingProvider.getTagType(typeName);

            Constituent constAnno = (Constituent) aJCas.getCas().createAnnotation(constType, 0, 0);
            constAnno.setConstituentType(typeName);

            // link to parent
            if (aParentFS != null) {
                constAnno.setParent(aParentFS);
            }

            // Do we have any children?
            List<Annotation> childAnnotations = new ArrayList<Annotation>();
            for (Tree<String> child : aNode.getChildren()) {
                Annotation childAnnotation = createConstituentAnnotationFromTree(aJCas, child,
                        constAnno, aTokens, aIndex);
                if (childAnnotation != null) {
                    childAnnotations.add(childAnnotation);
                }
            }
            if (childAnnotations.isEmpty()) {
                System.err.println("empty ca");
                for (Token t : aTokens)
                    System.err.print(t.getCoveredText() + " ");
                System.err.println(">");
                return null;
	    }

            constAnno.setBegin(childAnnotations.get(0).getBegin());
            constAnno.setEnd(childAnnotations.get(childAnnotations.size() - 1).getEnd());

            // Now that we know how many children we have, link annotation of
            // current node with its children
            FSArray childArray = FSCollectionFactory.createFSArray(aJCas,
                    childAnnotations);
            constAnno.setChildren(childArray);

            // write annotation for current node to index
            aJCas.addFsToIndexes(constAnno);

            return constAnno;
        }
    }

    private class BerkeleyParserModelProvider
        extends ModelProviderBase<Parser>
    {
        {
            setContextObject(BerkeleyParser.this);

            setDefault(ARTIFACT_ID, "${groupId}.berkeleyparser-model-parser-${language}-${variant}");
            setDefault(LOCATION, "classpath:/${package}/lib/parser-${language}-${variant}.bin");
            setDefaultVariantsLocation("${package}/lib/parser-default-variants.map");

            setOverride(LOCATION, modelLocation);
            setOverride(LANGUAGE, language);
            setOverride(VARIANT, variant);
        }

        @Override
        protected Parser produceResource(URL aUrl)
            throws IOException
        {
            ObjectInputStream is = null;
            try {
                is = new ObjectInputStream(new GZIPInputStream(aUrl.openStream()));
                ParserData pData = (ParserData) is.readObject();

                Grammar grammar = pData.getGrammar();
                Lexicon lexicon = pData.getLexicon();
                Numberer.setNumberers(pData.getNumbs());

                double threshold = 1.0;

                Properties metadata = getResourceMetaData();
                SingletonTagset posTags = new SingletonTagset(
                        POS.class, metadata.getProperty("pos.tagset"));
                SingletonTagset constTags = new SingletonTagset(
                        Constituent.class, metadata.getProperty("constituent.tagset"));

                Numberer tagNumberer = (Numberer) pData.getNumbs().get("tags");
                for (int i = 0; i < tagNumberer.size(); i++) {
                    String tag = (String) tagNumberer.object(i);
                    if (!binarize && tag.startsWith("@")) {
                        continue; // Only show aux. binarization tags if it is enabled.
                    }
                    if (tag.endsWith("^g")) {
                        constTags.add(tag.substring(0, tag.length() - 2));
                    }
                    else if ("ROOT".equals(tag)) {
                        constTags.add(tag);
                    }
                    else {
                        posTags.add(tag);
                    }
                }

                addTagset(posTags, createPosTags);
                addTagset(constTags);

                if (printTagSet) {
                    getContext().getLogger().log(INFO, getTagset().toString());
                }

                return new CoarseToFineMaxRuleParser(grammar, lexicon, threshold, -1, viterbi,
                        substates, scores, accurate, variational, true, true);
            }
            catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
            finally {
                closeQuietly(is);
            }
        }
    };
}
