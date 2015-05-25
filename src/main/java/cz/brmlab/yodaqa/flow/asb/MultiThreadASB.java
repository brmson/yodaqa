/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package cz.brmlab.yodaqa.flow.asb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.asb.ASB;
import org.apache.uima.analysis_engine.asb.impl.FlowContainer;
import org.apache.uima.analysis_engine.asb.impl.FlowControllerContainer;
import org.apache.uima.analysis_engine.impl.AggregateAnalysisEngine_impl;
import org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase;
import org.apache.uima.analysis_engine.impl.AnalysisEngineManagementImpl;
import org.apache.uima.analysis_engine.impl.EmptyCasIterator;
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.analysis_engine.metadata.impl.AnalysisEngineMetaData_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.ParallelStep;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.SimpleStepWithResultSpec;
import org.apache.uima.flow.Step;
import org.apache.uima.flow.impl.FlowControllerContext_impl;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.Resource_ImplBase;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.Level;
import org.apache.uima.util.UimaTimer;

/**
 * An ASB that works like the original UIMA one, but uses multi-threading
 * whenever possible. Analysis Structure Broker (ASB) is the thing actually
 * executing the pipeline, based on AnalysisEngine descriptors and flow
 * steps.
 *
 * There are two basic cases of multi-threading opportunities
 * within UIMA:
 *
 * - SimpleStep flow (a "straight" pipeline) with an analysis
 *   engine that has PARAM_NUM_SIMULTANEOUS_REQUESTS set; then,
 *   multiple threads would be able to process() through this
 *   engine (which is in fact instantiated multiple times, but
 *   this is hidden through a MultiprocessingAnalysisEngine
 *   abstraction); so, we can e.g. have this parameter set
 *   for PassageAnalysisAE or AnswerAnalysisAE to analyze
 *   multiple passages/answers at once.
 *
 * - ParallelStep flow (a "multi-tier" pipeline) with multiple
 *   analysis engines that each run logically in parallel and
 *   we want to run them in parallel in practice as well;
 *   so, we would be doing all our search types at once.
 *
 * In the end, we want to support both parallelizations
 * (using a common thread pool), but at first we do the
 * ParallelStep one.
 *
 * Also, note that there are inherent barriers at AnalysisEngine
 * boundaries; if multiple of your AEs run in parallel at one point
 * and each returns a bunch of CASes, neither of them will continue
 * through the pipeline until all of the AEs are finished.
 *
 * (TODO: Corollary - Move AnswerAnalysis to a tail section of each
 * AnswerProducer so that we don't block on this barrier.)
 *
 * Corollary #2 - We create many CASes in memory before getting to
 * merge them back post-barrier.  This is a problem with the default
 * UIMA CasManager as it creates a single-CAS pool and hangs forever
 * when we want to get another CAS before returning the first one.
 * Our current workaround is for parallel-running CAS multipliers
 * to have a custom method getCasInstancesRequired() that returns
 * something much larger than one (512).  TODO make own CasManager
 * with dynamically-sized pools.
 *
 * N.B. the multi-threading pool is common for the whole Java program,
 * being a static member of this ASB. TODO #threads
 *
 * XXX: This is a copy of UIMA's ASB_impl that adds some extra methods
 * and classes, but otherwise just changes the processAndOutputNewCASes()
 * method.  Due to extensive use of private fields, it is impractical
 * to sub-class it instead. */
public class MultiThreadASB extends Resource_ImplBase implements ASB {
  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "cz.brmlab.yodaqa.flow";

  /**
   * current class
   */
  private static final Class<MultiThreadASB> CLASS_NAME = MultiThreadASB.class;

  /**
   * Map from String key to delegate AnalysisEngine for all component AnalysisEngines within this
   * ASB.
   */
  private Map<String, AnalysisEngine> mComponentAnalysisEngineMap = new LinkedHashMap<String, AnalysisEngine>();

  /**
   * Map from String key to delegate AnalysisEngineMetaData for all component AnalysisEngines within
   * this ASB.
   */
  private Map<String, AnalysisEngineMetaData> mComponentAnalysisEngineMetaDataMap =
      new HashMap<String, AnalysisEngineMetaData>();

  /**
   * Map from String key to component (AnalysisEngine or FlowController) metadata.
   */
  private Map<String, ProcessingResourceMetaData> mAllComponentMetaDataMap = new LinkedHashMap<String, ProcessingResourceMetaData>();

  /**
   * Initialization parameters passed to this ASB's initialize method. They will be passed along to
   * the delegate AnalysisEngines.
   */
  private Map<String, Object> mInitParams;

  private SofaMapping[] mSofaMappings;

  private FlowControllerContainer mFlowControllerContainer;

  /**
   * Whether this aggregate is declared to output new CASes.
   */
  private boolean mOutputNewCASes;

  /**
   * UimaContext of the Aggregate AE containing this ASB.
   */
  private UimaContextAdmin mAggregateUimaContext;

  /**
   * The pool of worker threads running parallelizable annotators.
   */
  protected static ExecutorService parallelExecutor = Executors.newFixedThreadPool(3);

  /**
   * Initializes this ASB.
   * 
   * @param aSpecifier
   *          describes how to create this ASB.
   * @param aAdditionalParams
   *          parameters which are passed along to the delegate Analysis Engines when they are
   *          constructed
   * 
   * @return true if and only if initialization completed successfully. Returns false if this
   *         implementation cannot handle the given <code>ResourceSpecifier</code>.
   * 
   * @see org.apache.uima.resource.Resource#initialize(ResourceSpecifier, Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    UIMAFramework.getLogger(CLASS_NAME).logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize",
            LOG_RESOURCE_BUNDLE, "UIMA_asb_init_begin__CONFIG");
    if (!(aSpecifier instanceof ResourceCreationSpecifier)) {
      return false;
    }

    // copy the additional parameters, since this method will modify them
    super.initialize(aSpecifier, aAdditionalParams = new HashMap<String, Object>(aAdditionalParams));

    // save parameters for later
    mInitParams = aAdditionalParams;

    // save the sofa mappings of the aggregate AE that this AE is part of
    mSofaMappings = (SofaMapping[]) mInitParams.remove(Resource.PARAM_AGGREGATE_SOFA_MAPPINGS);
    // also remove them from the aAdditionalParams map, as they don't need to be passed
    // on to delegates
    // if (mSofaMappings != null)
    // mInitParams.remove(mInitParams.get(Resource.PARAM_AGGREGATE_SOFA_MAPPINGS));

    UIMAFramework.getLogger(CLASS_NAME).logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize",
            LOG_RESOURCE_BUNDLE, "UIMA_asb_init_successful__CONFIG");
    return true;
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    // destroy component AnalysisEngines that have been successfully initialized
    //   unsuccessful initializations are not put into the Map
    Iterator<Map.Entry<String, AnalysisEngine>> i = mComponentAnalysisEngineMap.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry<String, AnalysisEngine> entry = i.next();
      Resource delegate = entry.getValue();
      delegate.destroy();
    }
    
    if (mFlowControllerContainer != null &&
        // the container might be non-null, but the initialization could have failed
  // XXX: we disable this check for now as isInitialized() is not public
        true /*mFlowControllerContainer.isInitialized()*/) {
      mFlowControllerContainer.destroy();
    }

    parallelExecutor.shutdownNow();
  }

  /**
   * Called after calling initialize() (see above)
   * by the Aggregate Analysis Engine to provide this ASB with information it needs to
   * operate.
   * 
   * @param aSpecifiers
   *          the specifiers for all component AEs within this Aggregate. The ASB will instantiate
   *          those AEs.
   * @param aParentContext
   *          UIMA context for the aggregate AE
   * @param aFlowControllerDeclaration
   *          declaration (key and specifier) of FlowController to be used for this aggregate.
   * @param aAggregateMetadata metadata for the aggregate AE
   * @throws ResourceInitializationException passthru
   */
  public void setup(Map<String, ResourceSpecifier> aSpecifiers, UimaContextAdmin aParentContext,
          FlowControllerDeclaration aFlowControllerDeclaration,
          AnalysisEngineMetaData aAggregateMetadata) throws ResourceInitializationException {
    mAggregateUimaContext = aParentContext;

    // clear the delegate AnalysisEngine and AnalysisEngineMetaData maps
    mComponentAnalysisEngineMap.clear();
    mComponentAnalysisEngineMetaDataMap.clear();
    mAllComponentMetaDataMap.clear();

    // loop through all entries in the (key, specifier) map
    Iterator<Map.Entry<String,ResourceSpecifier>> i = aSpecifiers.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry<String,ResourceSpecifier> entry = i.next();
      String key =entry.getKey();
      ResourceSpecifier spec = entry.getValue();

      Map<String, String> sofamap = new TreeMap<String, String>();

      // retrieve the sofa mappings for input/output sofas of this analysis engine
      if (mSofaMappings != null && mSofaMappings.length > 0) {
        for (int s = 0; s < mSofaMappings.length; s++) {
          // the mapping is for this analysis engine
          if (mSofaMappings[s].getComponentKey().equals(key)) {
            // if component sofa name is null, replace it with the default for CAS sofa name
            // This is to support single-view annotators.
            if (mSofaMappings[s].getComponentSofaName() == null)
              mSofaMappings[s].setComponentSofaName(CAS.NAME_DEFAULT_SOFA);
            sofamap.put(mSofaMappings[s].getComponentSofaName(), mSofaMappings[s]
                    .getAggregateSofaName());
          }
        }
      }

      // create child UimaContext and insert into mInitParams map
      // mInitParams was previously set to the value of aAdditionalParams
      //  passed to the initialize method of this aggregate, by the
      //  preceeding call to initialize().
      
      if (mInitParams == null)
        mInitParams = new HashMap<String, Object>();
      UimaContextAdmin childContext = aParentContext.createChild(key, sofamap);
      mInitParams.put(Resource.PARAM_UIMA_CONTEXT, childContext);

      AnalysisEngine ae;

      // if running in "validation mode", don't try to connect to any services
      if (mInitParams.containsKey(AnalysisEngineImplBase.PARAM_VERIFICATION_MODE)
              && !(spec instanceof ResourceCreationSpecifier)) {
        // but we need placeholder entries in maps to satisfy later checking
        ae = new DummyAnalysisEngine();
      } else {
        // construct an AnalysisEngine - initializing it with the parameters
        // passed to this ASB's initialize method
        ae = UIMAFramework.produceAnalysisEngine(spec, mInitParams);
      }

      // add the Analysis Engine and its metadata to the appropriate lists

      // add AnlaysisEngine to maps based on key
      mComponentAnalysisEngineMap.put(key, ae);
      mComponentAnalysisEngineMetaDataMap.put(key, ae.getAnalysisEngineMetaData());
    }

    // make Maps unmodifiable
    mComponentAnalysisEngineMap = Collections.unmodifiableMap(mComponentAnalysisEngineMap);
    mComponentAnalysisEngineMetaDataMap = Collections
            .unmodifiableMap(mComponentAnalysisEngineMetaDataMap);

    mOutputNewCASes = aAggregateMetadata.getOperationalProperties().getOutputsNewCASes();

    // initialize the FlowController
    initFlowController(aFlowControllerDeclaration, aParentContext, aAggregateMetadata);

    // initialize the AllComponentMetaData map to include AEs plus the FlowController
    mAllComponentMetaDataMap = new LinkedHashMap<String, ProcessingResourceMetaData>(mComponentAnalysisEngineMetaDataMap);
    mAllComponentMetaDataMap.put(aFlowControllerDeclaration.getKey(), mFlowControllerContainer
            .getProcessingResourceMetaData());
    mAllComponentMetaDataMap = Collections.unmodifiableMap(mAllComponentMetaDataMap);
  }

  /*
   * Initializes the FlowController for this aggregate.
   */
  protected void initFlowController(FlowControllerDeclaration aFlowControllerDeclaration,
          UimaContextAdmin aParentContext, AnalysisEngineMetaData aAggregateMetadata)
          throws ResourceInitializationException {
    String key = aFlowControllerDeclaration.getKey();
    if (key == null || key.length() == 0) {
      key = "_FlowController"; // default key
    }

    Map<String, Object> flowControllerParams = new HashMap<String, Object>(mInitParams);

    // retrieve the sofa mappings for the FlowControler
    Map<String, String> sofamap = new TreeMap<String, String>();
    if (mSofaMappings != null && mSofaMappings.length > 0) {
      for (int s = 0; s < mSofaMappings.length; s++) {
        // the mapping is for this analysis engine
        if (mSofaMappings[s].getComponentKey().equals(key)) {
          // if component sofa name is null, replace it with the default for TCAS sofa name
          // This is to support single-view annotators.
          if (mSofaMappings[s].getComponentSofaName() == null)
            mSofaMappings[s].setComponentSofaName(CAS.NAME_DEFAULT_SOFA);
          sofamap.put(mSofaMappings[s].getComponentSofaName(), mSofaMappings[s]
                  .getAggregateSofaName());
        }
      }
    }
    FlowControllerContext ctxt = new FlowControllerContext_impl(aParentContext, key, sofamap,
            getComponentAnalysisEngineMetaData(), aAggregateMetadata);
    flowControllerParams.put(PARAM_UIMA_CONTEXT, ctxt);
    flowControllerParams.put(PARAM_RESOURCE_MANAGER, getResourceManager());
    mFlowControllerContainer = new FlowControllerContainer();
    mFlowControllerContainer.initialize(aFlowControllerDeclaration.getSpecifier(),
            flowControllerParams);
  }

  /**
   * @see org.apache.uima.analysis_engine.asb.ASB#getComponentAnalysisEngineMetaData()
   */
  public Map<String, AnalysisEngineMetaData> getComponentAnalysisEngineMetaData() {
    return mComponentAnalysisEngineMetaDataMap;
  }

  /**
   * @see org.apache.uima.analysis_engine.asb.ASB#getComponentAnalysisEngines()
   */
  public Map<String, AnalysisEngine> getComponentAnalysisEngines() {
    return mComponentAnalysisEngineMap;
  }

  public Map<String, ProcessingResourceMetaData> getAllComponentMetaData() {
    return mAllComponentMetaDataMap;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.asb.ASB#process(org.apache.uima.cas.CAS)
   */
  public CasIterator process(CAS aCAS) throws AnalysisEngineProcessException {
    return new AggregateCasIterator(aCAS);
  }

  /** Not public API. Is declared public so it can be used by test case. */
  public FlowControllerContainer getFlowControllerContainer() {
    return mFlowControllerContainer;
  }

  /**
   * Gets the MBean that provides the management interface to this AE. Returns the same object as
   * UimaContext.getManagementInterface() but casted to the AnalysisEngineManagement type.
   * @return the MBean for the management interface to this AE
   */
  protected AnalysisEngineManagementImpl getMBean() {
    return (AnalysisEngineManagementImpl) mAggregateUimaContext.getManagementInterface();
  }

  /**
   * Inner class implementing the CasIterator returned from the processAndOutputNewCASes(CAS)
   * method. This class contains most of the execution control logic for the aggregate AE.
   * 
   */
  class AggregateCasIterator implements CasIterator {
    /** The CAS that was input to the Aggregate AE's process method. */
    CAS mInputCas;

    /**
     * Stack, which holds StackFrame objects. A stack is necessary to handle CasMultipliers, because
     * when a CasMultiplier is invoked we need to save the state of processing of the current CAS
     * and start processing the output CASes instead. Since CasMultipliers can be nested, we need a
     * stack.
     */
    Stack<StackFrame> casIteratorStack = new Stack<StackFrame>();

    /**
     * Set of CASes that are in circulation (that is, they have been passed to FlowController and
     * the FlowController hasn't yet returned a FinalStep for them). Needed so we can clean up on
     * error.
     */
    Set<CAS> activeCASes = new HashSet<CAS>();

    /** Holds the next CAS to be returned, if it is known. */
    CAS nextCas = null;

    /** timer for timing processing done during calls to next() */
    UimaTimer timer = UIMAFramework.newTimer();

    /**
     * Creates a new AggregateCasIterator for the given input CAS. The CasIterator will return all
     * of the output CASes that this Aggregate AE generates when run on that input CAS, if any.
     * 
     * @param inputCas
     *          the CAS to be input to the Aggregate AE (this is the CAS that was passed to the
     *          Aggregate AE's processAndOutputNewCASes(CAS) method)
     * @throws AnalysisEngineProcessException
     *           if processing fails
     */
    public AggregateCasIterator(CAS inputCas) throws AnalysisEngineProcessException {
      timer.startIt();
      try {
        mInputCas = inputCas;
        // compute the flow for this CAS
        FlowContainer flow = mFlowControllerContainer.computeFlow(inputCas);
        // store CAS and Flow in an initial stack frame which will later be read by the
        // processUtilNextOutputCas method.
        casIteratorStack.push(new StackFrame(new EmptyCasIterator(), inputCas, flow, null));
        // do the initial processing here (this will do all of the processing in the case
        // where this AE is not a CasMultiplier)
        nextCas = processUntilNextOutputCas();
        getMBean().incrementCASesProcessed();
      } finally {
        timer.stopIt();
        getMBean().reportAnalysisTime(timer.getDuration());
      }
    }

    /**
     * Returns whether there are any more CASes to be returned.
     */
    public boolean hasNext() throws AnalysisEngineProcessException {
      timer.startIt();
      try {
        if (nextCas == null)
          nextCas = processUntilNextOutputCas();
        return (nextCas != null);
      } finally {
        timer.stopIt();
        getMBean().reportAnalysisTime(timer.getDuration());
      }
    }

    /** Gets the next output CAS. */
    public CAS next() throws AnalysisEngineProcessException {
      timer.startIt();
      try {
        CAS toReturn = nextCas;
        if (toReturn == null)
          toReturn = processUntilNextOutputCas();
        if (toReturn == null) {
          throw new UIMA_IllegalStateException(UIMA_IllegalStateException.NO_NEXT_CAS,
                  new Object[0]);
        }
        nextCas = null;
        getMBean().incrementCASesProcessed();
        return toReturn;
      } finally {
        timer.stopIt();
        getMBean().reportAnalysisTime(timer.getDuration());
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.analysis_engine.CasIterator#release()
     */
    public void release() {
      // pop all frames off the casIteratorStack, calling Flow.abort() on flow objects and
      //CasIterator.release() on the CAS iterators
      while (!casIteratorStack.isEmpty()) {
        StackFrame frame = casIteratorStack.pop();
        frame.originalCasFlow.aborted();
        frame.casIterator.release();
      }
      
      // release all active, internal CASes
      Iterator<CAS> iter = activeCASes.iterator();
      while (iter.hasNext()) {
        CAS cas = iter.next();
        // mFlowControllerContainer.dropCas(cas);
        if (cas != mInputCas) // don't release the input CAS, it's caller's responsibility
        {
          cas.release();
        }
      }
      //clear the active CASes list, to guard against ever trying to
      //reuse these CASes or trying to release them a second time.
      activeCASes.clear();       
    }

    /** Pick the Cas to process (send through the flow) next.
     * Possibly sets its nextStep (XXX pass-by-reference hack) too. */
    protected CasInFlow nextCasToProcess(Step[] nextStepRef) throws Exception {
      CasInFlow cif = null;
      // get an initial CAS from the CasIteratorStack
      while (cif == null) {
        if (casIteratorStack.isEmpty()) {
          return null; // there are no more CAS Iterators to obtain CASes from
        }
        StackFrame frame = casIteratorStack.peek();
        try {
          if (frame.casIterator.hasNext()) {
            CAS cas = frame.casIterator.next();
            // this is a new output CAS so we need to compute a flow for it
            FlowContainer flow = frame.originalCasFlow.newCasProduced(cas, frame.casMultiplierAeKey);
            cif = new CasInFlow(cas, flow);
          }
        } 
        catch(Exception e) {
          //A CAS Multiplier (or possibly an aggregate) threw an exception trying to output the next CAS.
          //We abandon trying to get further output CASes from that CAS Multiplier,
          //and ask the Flow Controller if we should continue routing the CAS that was input to the CasMultiplier.
          if (!frame.originalCasFlow.continueOnFailure(frame.casMultiplierAeKey, e)) {
            throw e;              
          } else {
            UIMAFramework.getLogger(CLASS_NAME).logrb(Level.FINE, CLASS_NAME.getName(), "processUntilNextOutputCas",
                    LOG_RESOURCE_BUNDLE, "UIMA_continuing_after_exception__FINE", e);
           
          }
          //if the Flow says to continue, we fall through to the if (cif == null) block below, get
          //the originalCas from the stack and continue with its flow.
        }
        if (cif == null) {
          // we've finished routing all the Output CASes from a StackFrame. Now
          // get the originalCas (the one that was input to the CasMultiplier) from
          // that stack frame and continue with its flow
          casIteratorStack.pop(); // remove this state from the stack now
          CAS cas = frame.originalCas;
          if (cas == null) {
            // null means "continue with the next stack frame instead",
            // when we have the followup steps prepared in advance
            continue;
          }
          FlowContainer flow = frame.originalCasFlow;
          nextStepRef[0] = frame.incompleteParallelStep; //in case we need to resume a parallel step
          cas.setCurrentComponentInfo(null); // this CAS is done being processed by the previous AnalysisComponent
          cif = new CasInFlow(cas, flow);
        }
      }
      return cif;
    }

    /** Enqueue CAS-in-flow for processing by the nextAeKey engine. */
    protected Future<CasIterator> enqueueCasInFlow(CasInFlow cif, String nextAeKey, ResultSpecification rs)
        throws AnalysisEngineProcessException {
      final CAS inputCas = cif.cas;
      final AnalysisEngine nextAe = mComponentAnalysisEngineMap.get(nextAeKey);
      if (nextAe == null) {
        throw new AnalysisEngineProcessException(
                AnalysisEngineProcessException.UNKNOWN_ID_IN_SEQUENCE,
                new Object[] { nextAeKey });
      }

      if (rs != null)
        nextAe.setResultSpecification(rs);

      Callable<CasIterator> job = new Callable<CasIterator>() {
        public CasIterator call() throws Exception {
          // invoke next AE in flow
          /* N.B. exceptions thrown here are re-thrown in main thread
           * at .get() time */
          CasIterator casIter = nextAe.processAndOutputNewCASes(inputCas);
          return casIter;
        }
      };
      Future<CasIterator> f;
      if (nextAe instanceof AggregateAnalysisEngine_impl) {
        /* Do not waste threads on aggregate AEs; instead, just run it
         * "inline" in the main thread, using threads for their contents
         * instead. */
        f = new FutureTask<CasIterator>(job);
        ((FutureTask<CasIterator>) f).run();
      } else {
        /* Dispatch this job to the thread pool. */
        f = parallelExecutorCS.submit(job);
      }
      return f;
    }

    /** Collect CAS-in-flow from processing done by the nextAeKey engine.
     * @return a StackFrame with a casIter producing new CASes if the engine
     * produced any. */
    protected StackFrame collectCasInFlow(CasInFlow cif, Future<CasIterator> f, String nextAeKey) throws Exception {
      CasIterator casIter = null;
      try {
        casIter = f.get();
      }
      catch(Exception e) {
        //ask the FlowController if we should continue
        //TODO: should this be configurable?
        if (!cif.flow.continueOnFailure(nextAeKey, e)) {
          throw e;
        }
        else {
          UIMAFramework.getLogger(CLASS_NAME).logrb(Level.FINE, CLASS_NAME.getName(), "processUntilNextOutputCas",
                  LOG_RESOURCE_BUNDLE, "UIMA_continuing_after_exception__FINE", e);
        }
      }
      if (casIter != null && casIter.hasNext()) // new CASes are output
      {
        return new StackFrame(casIter, cif.cas, cif.flow, nextAeKey);
      } else {
        return null;
      }
    }

    /** Switch the CAS-in-flow currently being processed to the given
     * stack frame.  This happens when we have produced a new CAS;
     * we have stored the current CAS in the stack frame and will
     * return to it as soon as the flow of the new CAS finishes. */
    protected CasInFlow switchToNewCasInFlow(StackFrame frame) throws Exception {
      CasInFlow cif = null;
      try {
          CAS cas = frame.casIterator.next();
          // this is a new output CAS so we need to compute a flow for it
          FlowContainer flow = frame.originalCasFlow.newCasProduced(cas, frame.casMultiplierAeKey);
          cif = new CasInFlow(cas, flow);
      } 
      catch(Exception e) {
        //A CAS Multiplier (or possibly an aggregate) threw an exception trying to output the next CAS.
        //We abandon trying to get further output CASes from that CAS Multiplier,
        //and ask the Flow Controller if we should continue routing the CAS that was input to the CasMultiplier.
        if (!frame.originalCasFlow.continueOnFailure(frame.casMultiplierAeKey, e)) {
          throw e;              
        } else {
          UIMAFramework.getLogger(CLASS_NAME).logrb(Level.FINE, CLASS_NAME.getName(), "processUntilNextOutputCas",
                  LOG_RESOURCE_BUNDLE, "UIMA_continuing_after_exception__FINE", e);
        }
        //if the Flow says to continue, we fall through to the if (cas == null) block below, get
        //the originalCas from the stack and continue with its flow.
        //XXX: this is currently unsupported
      }
      activeCASes.add(cif.cas);
      return cif;
    }

    /** Perform a simple flow step with the Cas. */
    protected CasInFlow processSimpleStep(CasInFlow cif, SimpleStep nextStep) throws Exception {
      ResultSpecification rs = null;
      //check if we have to set result spec, to support capability language flow
      if (nextStep instanceof SimpleStepWithResultSpec) {
        rs = ((SimpleStepWithResultSpec)nextStep).getResultSpecification();
      }

      String nextAeKey = ((SimpleStep) nextStep).getAnalysisEngineKey();
      Future<CasIterator> future = enqueueCasInFlow(cif, nextAeKey, rs);

      StackFrame stackFrame = collectCasInFlow(cif, future, nextAeKey);
      if (stackFrame != null) {
        casIteratorStack.push(stackFrame);
        cif = switchToNewCasInFlow(stackFrame);
      } else {
        // no new CASes are output; this cas is done being processed
        // by that AnalysisEngine so clear the componentInfo
        cif.cas.setCurrentComponentInfo(null);
      }
      return cif;
    }

    /** Perform a parallel flow step with the Cas. */
    protected CasInFlow processParallelStep(CasInFlow cif, ParallelStep nextStep) throws Exception {
      //create modifiable list of destinations 
      List<String> destinations = new ArrayList<String>((nextStep).getAnalysisEngineKeys());
      //execute them
      List<Future<CasIterator>> futures = new ArrayList<>(destinations.size());
      for (String nextAeKey : destinations) {
        futures.add(enqueueCasInFlow(cif, nextAeKey, null));
      }
      //collect the output cases and finish the step
      int i = 0;
      boolean newCasesProduced = false;
      for (Future<CasIterator> f : futures) {
        String nextAeKey = destinations.get(i);
        StackFrame stackFrame = collectCasInFlow(cif, f, nextAeKey);
        if (stackFrame != null) {
          if (newCasesProduced) {
            /* Another engine in our step already produced a casIterator
             * as well.  By unsetting originalCas, we make sure to unwind
             * to that casIterator instead of ignoring it and falling
             * back to the input Cas.  No worries, we will eventually
             * get to the input Cas when the stack frame of the first
             * engine producing a casIterator gets its turn. */
            stackFrame.originalCas = null;
          }
          casIteratorStack.push(stackFrame);
          newCasesProduced = true;
        }
        i++;
      }
      if (newCasesProduced) {
        // now pick one of the output CAS and continue to route that one; we'll come back to the original cas sometime later
        StackFrame frame = casIteratorStack.peek();
        cif = switchToNewCasInFlow(frame);
      } else {
        // no new CASes are output; this cas is done being processed
        // by that AnalysisEngine so clear the componentInfo
        cif.cas.setCurrentComponentInfo(null);
      }
      return cif;
    }

    /**
     * This is the main execution control method for the aggregate AE. It is called by the
     * AggregateCasProcessorCasIterator.next() method. This runs the Aggregate, starting from its
     * current state, until such time as the FlowController indicates a CAS should be returned to
     * the caller. The AggregateCasIterator remembers the state, so calling this method a second
     * time will continue processing from where it left off.
     * 
     * @return the next CAS to be output. Returns null if the processing of the input CAS has
     *         completed.
     * 
     * @throws ProcessingException
     *           if a failure occurs during processing
     */
    protected CAS processUntilNextOutputCas() throws AnalysisEngineProcessException {
      while (true) {
        CasInFlow cif = null;
        try {
          // get the cas+flow to run
          Step[] nextStepRef = { null };
          cif = nextCasToProcess(nextStepRef);
          Step nextStep = nextStepRef[0];

          if (cif == null)
            return null;  // stack empty!

          // record active CASes in case we encounter an exception and need to release them
          activeCASes.add(cif.cas);

          // if we're not in the middle of parallel step already, ask the FlowController 
          // for the next step
          if (nextStep == null) {
            nextStep = cif.flow.next();
          }

          // repeat until we reach a FinalStep
          while (!(nextStep instanceof FinalStep)) {
            //Simple Step
            if (nextStep instanceof SimpleStep) {
              cif = processSimpleStep(cif, (SimpleStep) nextStep);
            } 
            //ParallelStep (TODO: refactor out common parts with SimpleStep?)
            else if (nextStep instanceof ParallelStep) {
              cif = processParallelStep(cif, (ParallelStep) nextStep);
            } else {
              throw new AnalysisEngineProcessException(
                      AnalysisEngineProcessException.UNSUPPORTED_STEP_TYPE, new Object[] { nextStep
                              .getClass() });
            }
            nextStep = cif.flow.next();
          }
          // FinalStep was returned from FlowController.
          // We're done with the CAS.
          assert (nextStep instanceof FinalStep);
          FinalStep finalStep = (FinalStep) nextStep;
          activeCASes.remove(cif.cas);
          // If this is the input CAS, just return null to indicate we're done
          // processing it. It is an error if the FlowController tried to drop this CAS.
          if (cif.cas == mInputCas) {
            if (finalStep.getForceCasToBeDropped()) {
              throw new AnalysisEngineProcessException(
                      AnalysisEngineProcessException.ILLEGAL_DROP_CAS, new Object[0]);
            }
            return null;
          }
          // Otherwise, this is a new CAS produced within this Aggregate. We may or
          // may not return it, depending on the setting of the outputsNewCASes operational
          // property in this AE's metadata, and on the value of FinalStep.forceCasToBeDropped
          if (mOutputNewCASes && !finalStep.getForceCasToBeDropped()) {
            return cif.cas;
          } else {
            cif.cas.release();
          }
        } catch (Exception e) {
          //notify Flow that processing has aborted on this CAS
          if (cif.flow != null) {
            cif.flow.aborted();
          }
          release(); // release held CASes before throwing exception
          if (e instanceof AnalysisEngineProcessException) {
            throw (AnalysisEngineProcessException) e;
          } else {
            throw new AnalysisEngineProcessException(e);
          }
        }
      }
    }
  }

  /**
   * A frame on the processing stack for this Aggregate AE. Each time processing encounters a
   * CasMultiplier, a new StackFrame is created to store the state associated with the processing of
   * output CASes produced by that CasMultiplier.
   */
  static class StackFrame {
    StackFrame(CasIterator casIterator, CAS originalCas, FlowContainer originalCasFlow,
            String lastAeKey) {
      this(casIterator, originalCas, originalCasFlow, lastAeKey, null);
    }

    StackFrame(CasIterator casIterator, CAS originalCas, FlowContainer originalCasFlow,
            String lastAeKey, ParallelStep incompleteParallelStep) {
      this.casIterator = casIterator;
      this.originalCas = originalCas;
      this.originalCasFlow = originalCasFlow;
      this.casMultiplierAeKey = lastAeKey;
      this.incompleteParallelStep = incompleteParallelStep;
    }
    /** CasIterator that returns output CASes produced by the CasMultiplier. */
    CasIterator casIterator;

    /** The CAS that was passed as input to the CasMultiplier. */
    CAS originalCas;

    /**
     * The Flow object for the original CAS, so we can pick up processing from there once we've
     * processed all the Output CASes.
     */
    FlowContainer originalCasFlow;

    /** The key that identifies the CasMultiplier whose output we are processing */
    String casMultiplierAeKey;
    
    /** If the CAS Multiplier was called while processing a ParallelStep, this specifies
     * the remaining parts of the parallel step, so we can pick up processing from there
     * once we've processed all the output CASes.
     */
    ParallelStep incompleteParallelStep;
  }

  /**
   * A tuple of CAS and corresponding flow object.  This represents an en-route
   * CAS that is being processed (contrary to StackFrame which is a "stored
   * state" of a CAS which had its routing interrupted for the sake of some
   * children CASes spawned by a CasMultiplier). */
  static class CasInFlow {
    CAS cas;
    FlowContainer flow;

    public CasInFlow(CAS cas, FlowContainer flow) {
      this.cas = cas;
      this.flow = flow;
    }
  }

  /**
   * Dummy analysis engine to use in place of remote AE when in "verification mode".
   */
  private static class DummyAnalysisEngine extends PrimitiveAnalysisEngine_impl {
    public DummyAnalysisEngine() {
      setMetaData(new AnalysisEngineMetaData_impl());
    }
  }
}
