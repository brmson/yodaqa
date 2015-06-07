package cz.brmlab.yodaqa.flow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

/**
 * By "Philipp W".
 * 
 * https://groups.google.com/forum/#!topic/uimafit-users/yA0w2Q8tGNE
 *
 * "I played with this issue a bit more today and ended up writing an
 * implementation that runs a pipeline of analysis engines (similar to
 * SimplePipeline), but that supports things like CASMultipliers. I'm sure it
 * doesn't completely support all the variations of analysis engines that UIMA
 * theoretically offers (I've found the UIMA documentation badly lacking in
 * descriptions of the contracts that go with its interfaces), and I haven't
 * tested it beyond my straightforward use case, so I offer it as-is. Since I
 * don't have a developer's account for uimaFIT (and since I don't want to mess
 * with other people's code in SimplePipeline).
 *
 * I won't try to integrate this into the project, but if someone would like to
 * use all or part of it for uimaFIT please feel free to do so without
 * restrictions."
 *
 * Customized to a degree, e.g. fixed to work with Aggregate AEs.
 *
 * XXX: This code could probably do with quite a few cleanups. */

public final class MultiCASPipeline {
	public static void runPipeline(
			CollectionReaderDescription collectionReaderDescription,
			AnalysisEngineDescription ... analysisEngineDescriptions
			) throws UIMAException, IOException {

		/*
		 * Create a merged type system for all components in the pipeline
		 */
		List<TypeSystemDescription> typeSystemDescriptions = new ArrayList<TypeSystemDescription>();
		typeSystemDescriptions.add(collectionReaderDescription.getCollectionReaderMetaData().getTypeSystem());
		for( AnalysisEngineDescription analysisEngineDescription : analysisEngineDescriptions ) {
			typeSystemDescriptions.add(analysisEngineDescription.getAnalysisEngineMetaData().getTypeSystem());
		}

		TypeSystemDescription mergedTypeSystemDescription = CasCreationUtils.mergeTypeSystems(typeSystemDescriptions);


		/*
		 * Collect the metadata of all components (in order to create a CAS using CasCreationUtils),
		 * set each component's type system to the shared merged type system, and instantiate a
		 * CollectionReader / AnalysisEngine for each component.
		 */
		List<ResourceMetaData> analysisComponentsMetadata = new ArrayList<ResourceMetaData>();

		collectionReaderDescription.getCollectionReaderMetaData().setTypeSystem(mergedTypeSystemDescription);
		analysisComponentsMetadata.add(collectionReaderDescription.getMetaData());
		CollectionReader collectionReader = CollectionReaderFactory.createCollectionReader(collectionReaderDescription);

		List<AnalysisEngine> analysisEngines = new ArrayList<AnalysisEngine>();
		for( AnalysisEngineDescription analysisEngineDescription : analysisEngineDescriptions ) {
			if (analysisEngineDescription.isPrimitive())
				analysisEngineDescription.getAnalysisEngineMetaData().setTypeSystem(mergedTypeSystemDescription);
			analysisComponentsMetadata.add(analysisEngineDescription.getMetaData());

			AnalysisEngine analysisEngine = UIMAFramework.produceAnalysisEngine(analysisEngineDescription);
			analysisEngines.add(analysisEngine);
		}


		/*
		 * Run the pipeline
		 */
		CAS cas = null;
		while( collectionReader.hasNext() ) {
			if( cas == null ) // create a new CAS
				cas = CasCreationUtils.createCas(analysisComponentsMetadata);
			else // reuse the existing CAS to save resources
				cas.reset();

			collectionReader.getNext(cas);
			runAnalysisEngines(analysisEngines, 0, cas);
		}
		if( cas != null ) {
			cas.release();
		}

		/*
		 * Clean up: not completely sure if all of these need to be called, documentation on the calling
		 * protocols for UIMA interfaces is pretty spotty.
		 */
		collectionReader.close();
		collectionReader.destroy();

		for( AnalysisEngine analysisEngine : analysisEngines ) {
			analysisEngine.batchProcessComplete();
			analysisEngine.collectionProcessComplete();
			analysisEngine.destroy();
		}
	}

	private static void runAnalysisEngines(List<AnalysisEngine> analysisEngines, int index, CAS cas)
			throws AnalysisEngineProcessException {
		if( index >= analysisEngines.size() ) {
			// base case, this CAS has been run through to the end of the pipeline
			return;
		}

		// recursive case: run one AE, then do recursive call(s) on the rest

		AnalysisEngine analysisEngine = analysisEngines.get(index);
		if( ! analysisEngine.getAnalysisEngineMetaData().isSofaAware() ) {
			// I assume this is what should be done in this case. I don't want to mess with
			// SOFA mappings -- can use an aggregate analysis engine if that's necessary.
			cas = cas.getView(CAS.NAME_DEFAULT_SOFA);
		}

		if( analysisEngine.getAnalysisEngineMetaData().getOperationalProperties().getOutputsNewCASes() ) {
			// This could be a CasMultiplier, but the UIMA interface doesn't specify. All we
			// know is, this AE may output any number of CASes (including 0), and we need
			// to use a different "process" interface
			CasIterator casIterator = analysisEngine.processAndOutputNewCASes(cas);
			while( casIterator.hasNext() ) {
				// Do one recursive call on the rest of the pipeline for each CAS
				// that the analysis engine produces.
				CAS newCAS = casIterator.next();
				runAnalysisEngines(analysisEngines, index+1, newCAS);
				if( newCAS != cas ) {
					// If this is a new CAS produced by this analysis engine, we consider
					// ourselves responsible for cleaning it up. But if the AE just passed
					// on the CAS we gave it, we leave the clean up to our caller.
					newCAS.release();
				}
			}
		} else {
			// Run the CAS through this AE, then run the rest of the pipeline
			// recursively.
			analysisEngine.process(cas);
			runAnalysisEngines(analysisEngines, index+1, cas);
		}
	}
}
