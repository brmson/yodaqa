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

package cz.brmlab.yodaqa.flow;

import java.util.Iterator;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.flow.CasFlowController_ImplBase;
import org.apache.uima.flow.CasFlow_ImplBase;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.ParallelStep;
import org.apache.uima.flow.Step;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * A FlowController analog to FixedFlowController using ParallelStep.
 * Therefore, *all* components are invoked logically in parallel with
 * regard to the CAS flow.
 *
 * Note that if you are running multiple CAS multipliers in parallel
 * and are merging the CASes later, you are likely using some sort of
 * isLast marker in the last CASes produced. But since you have N
 * independent CAS multipliers here, you must modify your CAS merger
 * to finish processing only after encountering N isLast CASes.
 *
 * This code is an amalgamation of ParallelFlowController and
 * AdvancedFixedFlowController that are stock parts of UIMAJ's test suite.
 */
public class FixedParallelFlowController extends CasFlowController_ImplBase {
  public static final String PARAM_ACTION_AFTER_CAS_MULTIPLIER = "ActionAfterCasMultiplier";
  protected static final int ACTION_CONTINUE = 0;
  protected static final int ACTION_DROP = 2;
  protected static final int ACTION_DROP_IF_NEW_CAS_PRODUCED = 3;
  protected int mActionAfterCasMultiplier;

  public void initialize(FlowControllerContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);

    String actionAfterCasMultiplier = (String) aContext
            .getConfigParameterValue(PARAM_ACTION_AFTER_CAS_MULTIPLIER);
    if ("continue".equalsIgnoreCase(actionAfterCasMultiplier)) {
      mActionAfterCasMultiplier = ACTION_CONTINUE;
    } else if ("drop".equalsIgnoreCase(actionAfterCasMultiplier)) {
      mActionAfterCasMultiplier = ACTION_DROP;
    } else if ("dropIfNewCasProduced".equalsIgnoreCase(actionAfterCasMultiplier)) {
      mActionAfterCasMultiplier = ACTION_DROP_IF_NEW_CAS_PRODUCED;
    } else if (actionAfterCasMultiplier == null) {
      mActionAfterCasMultiplier = ACTION_DROP_IF_NEW_CAS_PRODUCED; // default
    } else {
      throw new ResourceInitializationException();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.flow.CasFlowController_ImplBase#computeFlow(org.apache.uima.cas.CAS)
   */
  public Flow computeFlow(CAS aCAS) throws AnalysisEngineProcessException {
    return new FixedParallelFlowObject();
  }

  class FixedParallelFlowObject extends CasFlow_ImplBase {
    private boolean wasPassedToCasMultiplier = false;
    private boolean casMultiplierProducedNewCas = false;
    private boolean internallyCreatedCas = false;
    private boolean firstStep = true;

    /**
     * Create a new parallel fixed flow.
     */
    public FixedParallelFlowObject() {
      // nop
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.flow.Flow#next()
     */
    public Step next() throws AnalysisEngineProcessException {
      if (!firstStep) {
        // time to produce a FinalStep
        if (wasPassedToCasMultiplier) {
          // if CAS was passed to a CAS multiplier on the last step, special processing
          // is needed according to the value of the ActionAfterCasMultiplier config parameter
          switch (mActionAfterCasMultiplier) {
            case ACTION_DROP:
              return new FinalStep(internallyCreatedCas);
            case ACTION_DROP_IF_NEW_CAS_PRODUCED:
              if (casMultiplierProducedNewCas) {
                return new FinalStep(internallyCreatedCas);
              }
              // else, continue with flow
              break;
              // if action is ACTION_CONTINUE, just continue with flow
          }
          wasPassedToCasMultiplier = false;
          casMultiplierProducedNewCas = false;
        }
        // ok, bye
        return new FinalStep();
      }

      // Produce a step that will run all AEs in parallel
      Set<String> AEkeys = getContext().getAnalysisEngineMetaDataMap().keySet();
      Step nextStep = new ParallelStep(AEkeys);

      // if next step is a CasMultiplier, set wasPassedToCasMultiplier to true for next time
      if (stepContainsCasMultiplier(nextStep))
        wasPassedToCasMultiplier = true;

      firstStep = false;

      // now send the CAS to the next AE(s) in sequence.
      return nextStep;
    }

    /**
     * @param nextStep
     * @return
     */
    private boolean stepContainsCasMultiplier(Step nextStep) {
      Iterator<String> iter = ((ParallelStep) nextStep).getAnalysisEngineKeys().iterator();
      while (iter.hasNext()) {
        String key = iter.next();
        AnalysisEngineMetaData md = (AnalysisEngineMetaData) getContext()
          .getAnalysisEngineMetaDataMap().get(key);
        if (md != null && md.getOperationalProperties() != null &&
            md.getOperationalProperties().getOutputsNewCASes())
          return true;
      }
      return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.flow.CasFlow_ImplBase#newCasProduced(CAS, String)
     */
    protected synchronized Flow newCasProduced(CAS newCas, String producedBy) throws AnalysisEngineProcessException {
      // record that the input CAS has been segmented (affects its subsequent flow)
      casMultiplierProducedNewCas = true;
      // it's a single-step flow, so new segments don't continue in the flow
      return new EmptyFlow();
    }
  }

  class EmptyFlow extends CasFlow_ImplBase {
    public Step next() {
      return new FinalStep();
    }
  }
}
