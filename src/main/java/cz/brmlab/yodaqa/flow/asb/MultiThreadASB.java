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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.concurrent.atomic.AtomicInteger;

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
 * An ASB that uses multi-threading whenever possible. Analysis Structure Broker
 * (ASB) is the thing actually executing the pipeline, based on AnalysisEngine
 * descriptors and flow steps.  This one uses an application-wide shared thread
 * pool of workers to execute Analysis Engines in parallel (either on the same
 * CAS in case of ParallelStep, or SimpleStepping different waiting CASes),
 * and one thread per CAS and aggregateAE that governs the flow of the CAS
 * through that AE asynchronously.
 *
 * See the README.md in the package directory for usage details, various
 * caveats, etc.
 *
 * N.B. the multi-threading pool is common for the whole Java program,
 * being a static member of this ASB.  By default, as many threads
 * as logical CPUs are spawned; set the YODAQA_N_THREADS environment
 * variable to use a different number.
 *
 * XXX: This is a copy of UIMA's ASB_impl that adds some substantial
 * modifications to the AggregateCasIterator.  Due to extensive use
 * of private fields, it is impractical to sub-class it instead.
 *
 * XXX: UIMA assumes that each CAS is processed by only one AE at once,
 * something which we explicitly *REFUSE* to honor.  This assumption applies
 * to read-only processing as well, as there may be AE-specific sofa name
 * mappings and class loaders involved.  Aside of not guaranteeing these
 * work, we need to also avoid race conditions e.g. in CASImpl.getView()
 * (vs. setCurrentComponentInfo(null)).  We do this by tactical usage of
 * synchronized(cas) blocks (marked as "setCurrentComponentInfo() critical
 * section") that should cover all conflicting calls (as of uimaj-2.5.0)
 * while keeping the parallelism at a useful level.
 * (Of your Analysis Component methods, only hasNext() has the input CAS
 * locked.)
 *
 * FIXME: Exception handling, as well as continueOnFailure(), is likely
 * quite messed up, sorry.  And who knows what would happen if you
 * use any timeout functionality. */
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
   * The pool of worker threads running parallelizable primitive (leaf)
   * annotators.
   */
  public static final int maxJobs;
  protected static final ExecutorService primitiveExecutor;
  static {
    String maxJobsEnv = System.getenv("YODAQA_N_THREADS");
    if (maxJobsEnv != null) {
      maxJobs = Integer.parseInt(maxJobsEnv);
    } else {
      int numProc = Runtime.getRuntime().availableProcessors();
      if (numProc >= 2) {
        /* This is (A) so that the user's machine is not swamped
         * (it also increases memory usage etc.), and (B) because
         * in train-and-eval we perform parallel train+test run
         * (but maybe we could abolish this now). */
        maxJobs = numProc / 2;
      } else {
        maxJobs = 1;
      }
    }
    primitiveExecutor = Executors.newFixedThreadPool(maxJobs);
  }

  /**
   * The pool of threads running aggregate analysis engines.
   * This means many threads, but all of them sleep almost all the time
   * (except when routing a CAS to the next primitive AE). */
  protected static final ExecutorService aggregateExecutor = Executors.newCachedThreadPool();

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

    primitiveExecutor.shutdownNow();
    aggregateExecutor.shutdownNow();
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
     * Stack frames associated with currently enqueued future jobs.
     * Used when retrieving the job result to continue the CAS flow.
     */
    protected Map<Future<CasIterator>, StackFrame> futureFrames = new HashMap<>();
    protected final AtomicInteger finishedJobs = new AtomicInteger(0);

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
        FlowContainer flow;
	synchronized (inputCas) {  // setCurrentComponentInfo() critical section
          flow = mFlowControllerContainer.computeFlow(inputCas);
        }
        // store CAS and Flow in an initial stack frame which will later be read by the
        // processUtilNextOutputCas method.
        CasInFlow cif = new CasInFlow(inputCas, flow);
        casIteratorStack.push(new StackFrame(new EmptyCasIterator(), cif, null));
        cif.depCounter += 1;
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
        frame.originalCIF.flow.aborted();
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

    /** Retrieve a new CAS-in-flow to process from the CasMultiplier stack.
     * @return null if no admissible CAS-in-flow is available on the stack. */
    protected CasInFlow casInFlowFromStack() throws Exception {
      if (casIteratorStack.isEmpty())
        return null;

      /* Get a new child CAS from the last suspended CAS multiplier. */
      StackFrame frame = casIteratorStack.peek();
      CasInFlow cif = newCasInFlowFromFrame(frame);
      if (cif != null) {
        trace("--- flow from stack " + cif.cas);
        return cif;
      }

      /* We've finished routing all the Output CASes from the StackFrame. */
      /* But do not retrieve the originalCIF of the deepest stack frame,
       * which represents the input CAS, while we still have pending futures.
       * Returning that CAS would indicate we are done processing. */
      if (casIteratorStack.size() == 1 && !futureFrames.keySet().isEmpty())
        return null;

      casIteratorStack.pop();
      frame.originalCIF.depCounter -= 1;
      cif = originalCasInFlowFromFrame(frame);
      if (cif == null) {
        /* The original CAS-in-flow cannot be resumed yet.
         * Proceed with the next stack frame. */
        return casInFlowFromStack();
      }
      /* We can resume routing the original CAS-in-flow. */
      return cif;
    }

    /** Produce a CAS-in-flow from a given job. */
    protected CasInFlow casInFlowFromFuture(Future<CasIterator> f) throws Exception {
      StackFrame frame = collectCasInFlow(f);
      if (frame.casIterator != null) {
        casIteratorStack.push(frame);
        frame.originalCIF.depCounter += 1;

        CasInFlow cif = newCasInFlowFromFrame(frame);
        if (cif != null) {
          trace("--- flow from future " + cif.cas);
          return cif;
        }

        casIteratorStack.pop();
        frame.originalCIF.depCounter -= 1;
      }
      return originalCasInFlowFromFrame(frame);
    }

    /** Pick the Cas to process (send through the flow) next.
     * This is either a finished async job, or a stacked CAS-in-flow. */
    protected CasInFlow nextCasToProcess() throws Exception {
      while (true) {
        trace("------------------- nextCasToProcess()" + " ["+this+"]");
        CasInFlow cif = null;

        /* First, check if some async job has been finished. */
        synchronized (finishedJobs) {
          while (finishedJobs.get() > 0) {
            for (Future<CasIterator> f : futureFrames.keySet()) {
              if (!f.isDone())
                continue;
              trace("--- flow from future");
	      try {
                cif = casInFlowFromFuture(f);
              } finally {
                finishedJobs.decrementAndGet();
              }
              if (cif != null)
                return cif;
            }

            /* XXX: When we wake up, the job is finished but it takes
             * a minuscule moment to mark the future as done - don't
             * spin meanwhile.
             * TODO rewrite this whole thing to use runnables and
             * queues instead of futures (I tried to use
             * CompletionService but I don't remember the exact
             * reasons of shelving it now). */
            Thread.sleep(1);
          }
        }

        /* Check if we can produce some new flows. */
        /* But don't needlessly drain CAS multipliers right away,
         * take only as many as we could possibly run at once (N.B.
         * in practice we will run less at once anyway, typically,
         * as multiple ASBs compete for the same thread pool).  */
        if (futureFrames.size() < maxJobs) {
          cif = casInFlowFromStack();
          if (cif != null) {
            trace("-- flow from stack " + cif.cas);
            return cif;
          }
        }

        trace("--- future " + futureFrames.keySet().size());

        /* Do we have anything to wait for? */
        if (futureFrames.keySet().isEmpty()) {
          if (casIteratorStack.size() > 0) {
            throw new Exception("no future but " + casIteratorStack.size() + " frames blocked on stack");
          }
          return null; // nope
        }

        /* Wait until some async job finishes. */
        synchronized (finishedJobs) {
          while (finishedJobs.get() == 0) {
	    /* When we get stuck for a while, it's time to explain
	     * the situation. */
	    long wait_start = System.currentTimeMillis();
	    int timeout_s = 5*60;

            finishedJobs.wait(timeout_s * 1000);

	    if (! (finishedJobs.get() == 0 && System.currentTimeMillis() - wait_start >= (timeout_s-1) * 1000))
              continue;

            System.err.println("ALERT: " + Thread.currentThread().getName() + " seems stuck for more than " + timeout_s + "s waiting for a job delivery.");
            dumpJobsState(System.err);

	    if (futureFrames.keySet().isEmpty()) {
	      System.err.println("ALERT: " + Thread.currentThread().getName() + " unexpectedly lost futureFrames.");
	      break;
	    }

            // XXX: This really triggers (albeit rarely, no idea why:( )
            for (Future<CasIterator> f : futureFrames.keySet()) {
              if (f.isDone()) {
                System.err.println("ALERT: " + Thread.currentThread().getName()
                      + " missed a finished " + futureFrames.get(f).casMultiplierAeKey
                      + " (finishedJobs counter out of sync), repairing...");
                finishedJobs.incrementAndGet();
              }
            }

            System.err.println();
	  }

          for (Future<CasIterator> f : futureFrames.keySet()) {
            if (!f.isDone())
              continue;
            trace("--- flow from future " + finishedJobs + " " + futureFrames.keySet().size());
            try {
              cif = casInFlowFromFuture(f);
            } finally {
              finishedJobs.decrementAndGet();
            }
            if (cif != null)
              return cif;
          }
        }
        // childless job, do another
        trace("--- loop");
      }
    }

    public void dumpJobsState(PrintStream out) {
      for (Future<CasIterator> f : futureFrames.keySet()) {
	StackFrame frame = futureFrames.get(f);
	out.println(Thread.currentThread() + "  - Future " + f
		    + " done " + f.isDone() + " canc " + f.isCancelled()
		    + " :: AE " + frame.casMultiplierAeKey
		    + " from CAS " + frame.originalCIF.cas
		    + " (CIF " + frame.originalCIF + ")");
      }
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
          trace("job start " + nextAe + " " + inputCas);
          CasIterator casIter = null;
          /* In case we are dealing with a NO_MULTIPROCESSING engine,
           * we need to synchronize calls to it.  It is not enough for
           * the AE implementation to have synchronized process():
           * Caused by: java.lang.NullPointerException
           *    at org.apache.uima.analysis_engine.impl.ResultSpecification_impl.intersect(ResultSpecification_impl.java:700)
           *    at org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl.callAnalysisComponentProcess(PrimitiveAnalysisEngine_impl.java:375)
           */
          try {
            if (nextAe instanceof PrimitiveAnalysisEngine_impl) {
              synchronized (nextAe) { casIter = nextAe.processAndOutputNewCASes(inputCas); }
            } else {
              casIter = nextAe.processAndOutputNewCASes(inputCas);
            }
          } finally {
            synchronized (finishedJobs) {
              finishedJobs.incrementAndGet();
              finishedJobs.notify();
            }
            trace("job finish " + nextAe + " " + inputCas);
          }
          return casIter;
        }
      };
      Future<CasIterator> f;
      if (nextAe instanceof AggregateAnalysisEngine_impl) {
        /* Spawn aggregate AEs into a dedicated thread, so that we do not
         * compute each single one in turn and at the sime time we do not
         * occupy worker threads which actually burn CPU time.  The aggregate
         * AE thread should be just sleeping all the time, so it's ok to
         * have many of them. */
        f = aggregateExecutor.submit(job);
        trace("job aggregate submit " + nextAe + " " + inputCas + " " + f);
      } else {
        /* Dispatch this job to the thread pool. */
        f = primitiveExecutor.submit(job);
        trace("job primitive submit " + nextAe + " " + inputCas + " " + f);
      }

      StackFrame frame = new StackFrame(null, cif, nextAeKey);
      futureFrames.put(f, frame);
      frame.originalCIF.depCounter += 1;

      return f;
    }

    /** Collect CAS-in-flow from processing done by the nextAeKey engine.
     * @return a StackFrame with a casIter producing new CASes. */
    protected StackFrame collectCasInFlow(Future<CasIterator> f) throws Exception {
      StackFrame frame = futureFrames.remove(f);
      trace("job collect " + frame.originalCIF.cas + " " + f);
      String nextAeKey = frame.casMultiplierAeKey; // TODO: rename (not necessarily casMultiplier)

      try {
        frame.casIterator = f.get();
      }
      catch(Exception e) {
        //ask the FlowController if we should continue
        //TODO: should this be configurable?
        if (!frame.originalCIF.flow.continueOnFailure(nextAeKey, e)) {
          throw e;
        }
        else {
          UIMAFramework.getLogger(CLASS_NAME).logrb(Level.FINE, CLASS_NAME.getName(), "processUntilNextOutputCas",
                  LOG_RESOURCE_BUNDLE, "UIMA_continuing_after_exception__FINE", e);
        }
      } finally {
        frame.originalCIF.depCounter -= 1;
      }

      return frame;
    }

    /** Check whether the given CasIterator has next,
     * thread-safely wrt given CAS. */
    protected boolean casIterHasNext(CasIterator iter, CAS cas) throws AnalysisEngineProcessException {
      if (iter instanceof AggregateCasIterator) {
        return iter.hasNext();
      } else synchronized (cas) {  // setCurrentComponentInfo() critical section
        return iter.hasNext();
      }
    }

    /** Extract a new child CAS-in-flow from the given stack frame.
     * This happens when we have produced a new CAS;
     * we have stored the current CAS in the stack frame and will
     * return to it as soon as the flow of the new CAS finishes. */
    protected CasInFlow newCasInFlowFromFrame(StackFrame frame) throws Exception {
      CasInFlow cif = null;
      try {
        CAS cas;
        /* Do not let two threads call hasNext(), then next() simultaneously. */
        synchronized (frame.casIterator) {
          if (!casIterHasNext(frame.casIterator, frame.originalCIF.cas))
            return null;
          cas = frame.casIterator.next();
        }
        // this is a new output CAS so we need to compute a flow for it
        FlowContainer flow = frame.originalCIF.flow.newCasProduced(cas, frame.casMultiplierAeKey);
        cif = new CasInFlow(cas, flow);
      } catch(Exception e) {
        //A CAS Multiplier (or possibly an aggregate) threw an exception trying to output the next CAS.
        //We abandon trying to get further output CASes from that CAS Multiplier,
        //and ask the Flow Controller if we should continue routing the CAS that was input to the CasMultiplier.
        if (!frame.originalCIF.flow.continueOnFailure(frame.casMultiplierAeKey, e)) {
          //notify Flow that processing has aborted on this CAS
          if (frame.originalCIF.flow != null)
            frame.originalCIF.flow.aborted();
          throw e;
        } else {
          UIMAFramework.getLogger(CLASS_NAME).logrb(Level.FINE, CLASS_NAME.getName(), "processUntilNextOutputCas",
                  LOG_RESOURCE_BUNDLE, "UIMA_continuing_after_exception__FINE", e);
        }
        //if the Flow says to continue, we fall through to the if (cas == null) block below, get
        //the original CAS-in-flow from the stack and continue with its flow.
        //XXX: this is currently unsupported
      }
      activeCASes.add(cif.cas);
      return cif;
    }

    /** Restore and get the original CAS-in-flow from a given frame.
     * @return null if that CAS-in-flow is blocked on something else yet. */
    protected CasInFlow originalCasInFlowFromFrame(StackFrame frame) {
      /* Now get the original CAS-in-flow (the one that was input to the
       * CasMultiplier) from that stack frame and continue with its flow
       * if admissible. */
      CasInFlow cif = frame.originalCIF;
      if (cif.depCounter > 0) {
        /* Do not restore the original CAS-in-flow, there is another
         * child frame referring it still hanging around. */
        trace("--- flow skip " + cif.depCounter + ", original dep block " + cif.cas + " " + cif);
        return null;
      }

      synchronized (cif.cas) {  // setCurrentComponentInfo() critical section
        cif.cas.setCurrentComponentInfo(null); // this CAS is done being processed by the previous AnalysisComponent
      }
      trace("--- flow back " + cif.depCounter + " to original " + cif.cas + " " + cif);
      return cif;
    }

    /** Perform a simple flow step with the Cas. */
    protected List<Future<CasIterator>> processSimpleStep(CasInFlow cif, SimpleStep nextStep) throws Exception {
      ResultSpecification rs = null;
      //check if we have to set result spec, to support capability language flow
      if (nextStep instanceof SimpleStepWithResultSpec) {
        rs = ((SimpleStepWithResultSpec)nextStep).getResultSpecification();
      }

      String nextAeKey = ((SimpleStep) nextStep).getAnalysisEngineKey();
      return Arrays.asList(enqueueCasInFlow(cif, nextAeKey, rs));
    }

    /** Perform a parallel flow step with the Cas. */
    protected List<Future<CasIterator>> processParallelStep(CasInFlow cif, ParallelStep nextStep) throws Exception {
      //create modifiable list of destinations 
      List<String> destinations = new ArrayList<String>((nextStep).getAnalysisEngineKeys());
      //execute them
      List<Future<CasIterator>> futures = new ArrayList<>(destinations.size());
      for (String nextAeKey : destinations) {
        futures.add(enqueueCasInFlow(cif, nextAeKey, null));
      }
      return futures;
    }

    /** Process a given CAS-in-flow, proceeding with steps in the flow
     * until we finish it.  N.B. if a CAS multiplier is part of the flow,
     * we will branch out into the child flow and stack away the parent
     * flow.
     * @return CAS finishing the flow; original CAS except when we
     *         switched to a child flow; null if this CAS was dropped
     *         or its flow was temporarily interrupted (by async job) */
    protected CAS processCasInFlow(CasInFlow cif) throws Exception {
      try {
        // ask the FlowController for the next step
        Step nextStep = cif.flow.next();

        // repeat until we reach a FinalStep
        while (!(nextStep instanceof FinalStep)) {
          Collection<Future<CasIterator>> futures;

          if (nextStep instanceof SimpleStep) {
            futures = processSimpleStep(cif, (SimpleStep) nextStep);

          } else if (nextStep instanceof ParallelStep) {
            futures = processParallelStep(cif, (ParallelStep) nextStep);

          } else {
            throw new AnalysisEngineProcessException(
                    AnalysisEngineProcessException.UNSUPPORTED_STEP_TYPE, new Object[] { nextStep
                            .getClass() });
          }

          // collect the output cases and finish the step
          boolean anyJobsFinished = false;
          boolean newCasesProduced = false;
          for (Future<CasIterator> f : futures) {
            if (!f.isDone())
              continue;
            anyJobsFinished = true;
            StackFrame stackFrame;
            try {
              stackFrame = collectCasInFlow(f);
            } finally {
              synchronized (finishedJobs) { finishedJobs.decrementAndGet(); }
            }

            if (stackFrame.casIterator != null) {
              casIteratorStack.push(stackFrame);
              cif.depCounter += 1;
              newCasesProduced = true;
            }
          }

          if (!anyJobsFinished) {
            // our jobs are all async, we cannot continue the flow
            // right now
            return null;
          } else if (newCasesProduced) {
            // give priority to the newly spawned cas;
            // we'll come back to the original cas sometime later,
            // the cif is saved in frame.originalCIF!
            // let nextCasToProcess() queue up the newly spawned
            // CASes to workers
            return null;
          } else {
            // no new CASes are output; this cas is done being processed
            // by that AnalysisEngine so clear the componentInfo
            synchronized (cif.cas) {  // setCurrentComponentInfo() critical section
              cif.cas.setCurrentComponentInfo(null);
            }
          }

          nextStep = cif.flow.next();
        }
        // FinalStep was returned from FlowController.
        // We're done with the CAS.
        assert (nextStep instanceof FinalStep);
        FinalStep finalStep = (FinalStep) nextStep;
        activeCASes.remove(cif.cas);

        if (finalStep.getForceCasToBeDropped()) {
          // If this is the input CAS, it is an error if the FlowController
          // tried to drop this CAS.
          if (cif.cas == mInputCas) {
            throw new AnalysisEngineProcessException(
                    AnalysisEngineProcessException.ILLEGAL_DROP_CAS, new Object[0]);
          }
          cif.cas.release();
          return null;
        }

        return cif.cas;
      } catch (Exception e) {
        //notify Flow that processing has aborted on this CAS
        if (cif.flow != null)
          cif.flow.aborted();
        throw e;
      }
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
      try {
        while (true) {
          // get the cas+flow to run
          CasInFlow cif = nextCasToProcess();

          if (cif == null) {
            trace("::: finished flow ["+this+"]");
            return null;  // stack empty!
          }

          trace("::: next step with " + cif.cas + " ["+this+"]");
          CAS outputCas = processCasInFlow(cif);

          // If this CAS has been dropped (FinalStep.forceCasToBeDropped),
          // or its flow was interrupted, just pick another one to route.
          if (outputCas == null)
            continue;

          // If this is the input CAS, just return null to indicate we're done
          // processing it.
          if (outputCas == mInputCas) {
            trace("::: finished flow by reaching the input ["+this+"]");
            return null;
          }
          // Otherwise, this is a new CAS produced within this Aggregate. We may or
          // may not return it, depending on the setting of the outputsNewCASes operational
          // property in this AE's metadata
          if (mOutputNewCASes) {
            return outputCas;
          } else {
            outputCas.release();
          }
        }
      } catch (Exception e) {
        release(); // release held CASes before throwing exception
        if (e instanceof AnalysisEngineProcessException) {
          throw (AnalysisEngineProcessException) e;
        } else {
          throw new AnalysisEngineProcessException(e);
        }
      }
    }

    protected void trace(String tracePrint) {
      String mtdebug = System.getProperty("cz.brmlab.yodaqa.mtdebug");
      if (mtdebug != null && !mtdebug.isEmpty())
        System.err.println(tracePrint);
    }
  }

  /**
   * A tuple of CAS and corresponding flow object.  This represents an en-route
   * CAS that is being processed. */
  static class CasInFlow {
    CAS cas;
    FlowContainer flow;

    /** Counter of prerequisites that need to be finished before
     * this CAS can continue its flow.  A routed CAS will have this
     * at zero, a CAS waiting for an async job will have this at 1,
     * a CAS suspended on the stack (due to a CasMultiplier producing
     * children CASes) will have this at 1 as well, but if the CAS
     * went through a ParallelStep, multiple CasMultipliers might act
     * on it and all of their flows need to finish before we can resume
     * this one, then this is greater than 1 and is decremented one-by-one
     * as stack frames referring this as their originalCIF are unwound. */
    int depCounter = 0;

    public CasInFlow(CAS cas, FlowContainer flow) {
      this.cas = cas;
      this.flow = flow;
    }
  }

  /**
   * A frame on the processing stack for this Aggregate AE. Each time processing
   * queues a job in the thread pool, a new StackFrame is created to store the
   * state associated with the processing of that CAS, to resume when the job
   * is finished.
   */
  static class StackFrame {
    StackFrame(CasIterator casIterator, CasInFlow originalCIF,
            String lastAeKey) {
      this.casIterator = casIterator;
      this.originalCIF = originalCIF;
      this.casMultiplierAeKey = lastAeKey;
    }
    /** CasIterator that returns output CASes produced by a CasMultiplier.
     * null if no new CASes are produced. */
    CasIterator casIterator;

    /** The CAS-in-flow suspended in this stack frame. */
    CasInFlow originalCIF;

    /** The key that identifies the CasMultiplier whose output we are processing.
     * TODO rename, not necessarily casMultiplier. */
    String casMultiplierAeKey;
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
