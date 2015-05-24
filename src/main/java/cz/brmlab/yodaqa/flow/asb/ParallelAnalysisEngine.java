package cz.brmlab.yodaqa.flow.asb;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.asb.ASB;
import org.apache.uima.analysis_engine.impl.AggregateAnalysisEngine_impl;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.impl.ResourceCreationSpecifier_impl;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.impl.ResourceMetaData_impl;
import org.apache.uima.util.InvalidXMLException;

/** A variant of AggregateAnalysisEngine that uses MultiThreadASB
 * instead of the basic ASB.  It is almost the same, but uses
 * a thread pool to spread logically parallelizable work.
 *
 * XXX: Ideally, UIMA would rather let us specify the ASB as part
 * of AnalysisEngineDescription; then, we would not need this class
 * at all. */

public class ParallelAnalysisEngine extends AggregateAnalysisEngine_impl {
	/**
	 * For an aggregate AnalysisEngine only, the ASB used to communicate
	 * with the delegate AnalysisEngines.
	 */
	protected ASB mMTASB;

	/**
	 * For an aggregate AnalysisEngine only, a Map from each component's key to a
	 * ProcessingResourceMetaData object for that component. This includes component AEs as well as
	 * the FlowController.
	 */
	protected Map<String, ProcessingResourceMetaData> mMTComponentMetaData;

	public void destroy() {
		if (mMTASB != null)
			mMTASB.destroy();
		super.destroy();
	}

	protected ASB _getASB() {
		return mMTASB;
	}

	protected Map<String, ProcessingResourceMetaData> _getComponentMetaData() {
		return mMTComponentMetaData;
	}

	protected void initASB(AnalysisEngineDescription aAnalysisEngineDescription, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		/* This is supposed to be IDENTICAL to
		 * 	AggregateAnalysisEngine_impl.initASB()
		 * besides a marked region; any differences are probably
		 * unintentional. */

		// add this analysis engine's name to the parameters sent to the ASB
		Map<String, Object> asbParams = new HashMap<String, Object>(aAdditionalParams);
		asbParams.put(ASB.PARAM_AGGREGATE_ANALYSIS_ENGINE_NAME, this.getMetaData().getName());  // not used 9/2013 scan
		asbParams.put(Resource.PARAM_RESOURCE_MANAGER, getResourceManager());

		// Pass sofa mappings defined in this aggregate as additional ASB parameters
		// System.out.println("remapping sofa names");
		asbParams.put(Resource.PARAM_AGGREGATE_SOFA_MAPPINGS, aAnalysisEngineDescription
				.getSofaMappings());

		// Get FlowController specifier from the aggregate descriptor. If none, use
		// default FixedFlow specifier.
		FlowControllerDeclaration flowControllerDecl = aAnalysisEngineDescription
			.getFlowControllerDeclaration();
		if (flowControllerDecl != null) {
			try {
				aAnalysisEngineDescription.getFlowControllerDeclaration().resolveImports(
						getResourceManager());
			} catch (InvalidXMLException e) {
				throw new ResourceInitializationException(e);
			}
		} else {
			flowControllerDecl = getDefaultFlowControllerDeclaration();
		}

		// create and configure ASB
		/* *** THIS *** is changed to instantiate the right class. */
		mMTASB = new MultiThreadASB();

		ResourceCreationSpecifier_impl dummyAsbSpecifier = new ResourceCreationSpecifier_impl();
		dummyAsbSpecifier.setMetaData(new ResourceMetaData_impl());
		/* *** THIS *** is changed to use _getASB(). */
		_getASB().initialize(dummyAsbSpecifier, asbParams);
		_getASB().setup(_getComponentCasProcessorSpecifierMap(), getUimaContextAdmin(), flowControllerDecl,
				getAnalysisEngineMetaData());


		// Get delegate AnalysisEngine metadata from the ASB
		/* *** THIS *** is changed to use _getASB() and a protected field. */
		mMTComponentMetaData = _getASB().getAllComponentMetaData();
	}
}
