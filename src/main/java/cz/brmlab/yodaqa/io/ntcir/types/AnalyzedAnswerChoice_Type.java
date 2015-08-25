
/* First created by JCasGen Mon Aug 04 11:44:10 EDT 2014 */
package cz.brmlab.yodaqa.io.ntcir.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Mon Aug 04 11:44:10 EDT 2014
 * @generated */
public class AnalyzedAnswerChoice_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (AnalyzedAnswerChoice_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = AnalyzedAnswerChoice_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new AnalyzedAnswerChoice(addr, AnalyzedAnswerChoice_Type.this);
  			   AnalyzedAnswerChoice_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new AnalyzedAnswerChoice(addr, AnalyzedAnswerChoice_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = AnalyzedAnswerChoice.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
 
  /** @generated */
  final Feature casFeat_qId;
  /** @generated */
  final int     casFeatCode_qId;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getQId(int addr) {
        if (featOkTst && casFeat_qId == null)
      jcas.throwFeatMissing("qId", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return ll_cas.ll_getStringValue(addr, casFeatCode_qId);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setQId(int addr, String v) {
        if (featOkTst && casFeat_qId == null)
      jcas.throwFeatMissing("qId", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    ll_cas.ll_setStringValue(addr, casFeatCode_qId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_ansChoiceId;
  /** @generated */
  final int     casFeatCode_ansChoiceId;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getAnsChoiceId(int addr) {
        if (featOkTst && casFeat_ansChoiceId == null)
      jcas.throwFeatMissing("ansChoiceId", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return ll_cas.ll_getStringValue(addr, casFeatCode_ansChoiceId);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAnsChoiceId(int addr, String v) {
        if (featOkTst && casFeat_ansChoiceId == null)
      jcas.throwFeatMissing("ansChoiceId", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    ll_cas.ll_setStringValue(addr, casFeatCode_ansChoiceId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_topic;
  /** @generated */
  final int     casFeatCode_topic;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTopic(int addr) {
        if (featOkTst && casFeat_topic == null)
      jcas.throwFeatMissing("topic", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return ll_cas.ll_getStringValue(addr, casFeatCode_topic);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTopic(int addr, String v) {
        if (featOkTst && casFeat_topic == null)
      jcas.throwFeatMissing("topic", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    ll_cas.ll_setStringValue(addr, casFeatCode_topic, v);}
    
  
 
  /** @generated */
  final Feature casFeat_highLevelContext;
  /** @generated */
  final int     casFeatCode_highLevelContext;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getHighLevelContext(int addr) {
        if (featOkTst && casFeat_highLevelContext == null)
      jcas.throwFeatMissing("highLevelContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return ll_cas.ll_getStringValue(addr, casFeatCode_highLevelContext);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHighLevelContext(int addr, String v) {
        if (featOkTst && casFeat_highLevelContext == null)
      jcas.throwFeatMissing("highLevelContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    ll_cas.ll_setStringValue(addr, casFeatCode_highLevelContext, v);}
    
  
 
  /** @generated */
  final Feature casFeat_specificContext;
  /** @generated */
  final int     casFeatCode_specificContext;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getSpecificContext(int addr) {
        if (featOkTst && casFeat_specificContext == null)
      jcas.throwFeatMissing("specificContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return ll_cas.ll_getStringValue(addr, casFeatCode_specificContext);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSpecificContext(int addr, String v) {
        if (featOkTst && casFeat_specificContext == null)
      jcas.throwFeatMissing("specificContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    ll_cas.ll_setStringValue(addr, casFeatCode_specificContext, v);}
    
  
 
  /** @generated */
  final Feature casFeat_assertionList;
  /** @generated */
  final int     casFeatCode_assertionList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAssertionList(int addr) {
        if (featOkTst && casFeat_assertionList == null)
      jcas.throwFeatMissing("assertionList", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return ll_cas.ll_getRefValue(addr, casFeatCode_assertionList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAssertionList(int addr, int v) {
        if (featOkTst && casFeat_assertionList == null)
      jcas.throwFeatMissing("assertionList", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    ll_cas.ll_setRefValue(addr, casFeatCode_assertionList, v);}
    
  
 
  /** @generated */
  final Feature casFeat_questionType;
  /** @generated */
  final int     casFeatCode_questionType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getQuestionType(int addr) {
        if (featOkTst && casFeat_questionType == null)
      jcas.throwFeatMissing("questionType", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return ll_cas.ll_getStringValue(addr, casFeatCode_questionType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setQuestionType(int addr, String v) {
        if (featOkTst && casFeat_questionType == null)
      jcas.throwFeatMissing("questionType", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    ll_cas.ll_setStringValue(addr, casFeatCode_questionType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_instruction;
  /** @generated */
  final int     casFeatCode_instruction;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getInstruction(int addr) {
        if (featOkTst && casFeat_instruction == null)
      jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return ll_cas.ll_getRefValue(addr, casFeatCode_instruction);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setInstruction(int addr, int v) {
        if (featOkTst && casFeat_instruction == null)
      jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    ll_cas.ll_setRefValue(addr, casFeatCode_instruction, v);}
    
  
 
  /** @generated */
  final Feature casFeat_questionContext;
  /** @generated */
  final int     casFeatCode_questionContext;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getQuestionContext(int addr) {
        if (featOkTst && casFeat_questionContext == null)
      jcas.throwFeatMissing("questionContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return ll_cas.ll_getRefValue(addr, casFeatCode_questionContext);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setQuestionContext(int addr, int v) {
        if (featOkTst && casFeat_questionContext == null)
      jcas.throwFeatMissing("questionContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    ll_cas.ll_setRefValue(addr, casFeatCode_questionContext, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public AnalyzedAnswerChoice_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_qId = jcas.getRequiredFeatureDE(casType, "qId", "uima.cas.String", featOkTst);
    casFeatCode_qId  = (null == casFeat_qId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_qId).getCode();

 
    casFeat_ansChoiceId = jcas.getRequiredFeatureDE(casType, "ansChoiceId", "uima.cas.String", featOkTst);
    casFeatCode_ansChoiceId  = (null == casFeat_ansChoiceId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_ansChoiceId).getCode();

 
    casFeat_topic = jcas.getRequiredFeatureDE(casType, "topic", "uima.cas.String", featOkTst);
    casFeatCode_topic  = (null == casFeat_topic) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_topic).getCode();

 
    casFeat_highLevelContext = jcas.getRequiredFeatureDE(casType, "highLevelContext", "uima.cas.String", featOkTst);
    casFeatCode_highLevelContext  = (null == casFeat_highLevelContext) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_highLevelContext).getCode();

 
    casFeat_specificContext = jcas.getRequiredFeatureDE(casType, "specificContext", "uima.cas.String", featOkTst);
    casFeatCode_specificContext  = (null == casFeat_specificContext) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_specificContext).getCode();

 
    casFeat_assertionList = jcas.getRequiredFeatureDE(casType, "assertionList", "uima.cas.FSList", featOkTst);
    casFeatCode_assertionList  = (null == casFeat_assertionList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_assertionList).getCode();

 
    casFeat_questionType = jcas.getRequiredFeatureDE(casType, "questionType", "uima.cas.String", featOkTst);
    casFeatCode_questionType  = (null == casFeat_questionType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_questionType).getCode();

 
    casFeat_instruction = jcas.getRequiredFeatureDE(casType, "instruction", "edu.cmu.lti.ntcir.qalab.types.Instruction", featOkTst);
    casFeatCode_instruction  = (null == casFeat_instruction) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_instruction).getCode();

 
    casFeat_questionContext = jcas.getRequiredFeatureDE(casType, "questionContext", "edu.cmu.lti.ntcir.qalab.types.Data", featOkTst);
    casFeatCode_questionContext  = (null == casFeat_questionContext) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_questionContext).getCode();

  }
}



    