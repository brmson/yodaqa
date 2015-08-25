

/* First created by JCasGen Mon Aug 04 11:44:10 EDT 2014 */
package cz.brmlab.yodaqa.io.ntcir.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Mon Aug 04 11:44:10 EDT 2014
 * XML source: /home/diwang/Dropbox/oaqa-workspace/ntcir-qalab-cmu-baseline/src/main/resources/WorldHistoryTypesDescriptor.xml
 * @generated */
public class AnalyzedAnswerChoice extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(AnalyzedAnswerChoice.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected AnalyzedAnswerChoice() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public AnalyzedAnswerChoice(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public AnalyzedAnswerChoice(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public AnalyzedAnswerChoice(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: qId

  /** getter for qId - gets 
   * @generated
   * @return value of the feature 
   */
  public String getQId() {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_qId == null)
      jcasType.jcas.throwFeatMissing("qId", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_qId);}
    
  /** setter for qId - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setQId(String v) {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_qId == null)
      jcasType.jcas.throwFeatMissing("qId", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    jcasType.ll_cas.ll_setStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_qId, v);}    
   
    
  //*--------------*
  //* Feature: ansChoiceId

  /** getter for ansChoiceId - gets 
   * @generated
   * @return value of the feature 
   */
  public String getAnsChoiceId() {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_ansChoiceId == null)
      jcasType.jcas.throwFeatMissing("ansChoiceId", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_ansChoiceId);}
    
  /** setter for ansChoiceId - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAnsChoiceId(String v) {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_ansChoiceId == null)
      jcasType.jcas.throwFeatMissing("ansChoiceId", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    jcasType.ll_cas.ll_setStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_ansChoiceId, v);}    
   
    
  //*--------------*
  //* Feature: topic

  /** getter for topic - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTopic() {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_topic == null)
      jcasType.jcas.throwFeatMissing("topic", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_topic);}
    
  /** setter for topic - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTopic(String v) {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_topic == null)
      jcasType.jcas.throwFeatMissing("topic", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    jcasType.ll_cas.ll_setStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_topic, v);}    
   
    
  //*--------------*
  //* Feature: highLevelContext

  /** getter for highLevelContext - gets 
   * @generated
   * @return value of the feature 
   */
  public String getHighLevelContext() {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_highLevelContext == null)
      jcasType.jcas.throwFeatMissing("highLevelContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_highLevelContext);}
    
  /** setter for highLevelContext - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHighLevelContext(String v) {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_highLevelContext == null)
      jcasType.jcas.throwFeatMissing("highLevelContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    jcasType.ll_cas.ll_setStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_highLevelContext, v);}    
   
    
  //*--------------*
  //* Feature: specificContext

  /** getter for specificContext - gets 
   * @generated
   * @return value of the feature 
   */
  public String getSpecificContext() {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_specificContext == null)
      jcasType.jcas.throwFeatMissing("specificContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_specificContext);}
    
  /** setter for specificContext - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSpecificContext(String v) {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_specificContext == null)
      jcasType.jcas.throwFeatMissing("specificContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    jcasType.ll_cas.ll_setStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_specificContext, v);}    
   
    
  //*--------------*
  //* Feature: assertionList

  /** getter for assertionList - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAssertionList() {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_assertionList == null)
      jcasType.jcas.throwFeatMissing("assertionList", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_assertionList)));}
    
  /** setter for assertionList - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAssertionList(FSList v) {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_assertionList == null)
      jcasType.jcas.throwFeatMissing("assertionList", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    jcasType.ll_cas.ll_setRefValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_assertionList, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: questionType

  /** getter for questionType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getQuestionType() {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_questionType == null)
      jcasType.jcas.throwFeatMissing("questionType", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_questionType);}
    
  /** setter for questionType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setQuestionType(String v) {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_questionType == null)
      jcasType.jcas.throwFeatMissing("questionType", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    jcasType.ll_cas.ll_setStringValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_questionType, v);}    
   
    
  //*--------------*
  //* Feature: instruction

  /** getter for instruction - gets 
   * @generated
   * @return value of the feature 
   */
  public Instruction getInstruction() {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_instruction == null)
      jcasType.jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return (Instruction)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_instruction)));}
    
  /** setter for instruction - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setInstruction(Instruction v) {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_instruction == null)
      jcasType.jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    jcasType.ll_cas.ll_setRefValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_instruction, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: questionContext

  /** getter for questionContext - gets 
   * @generated
   * @return value of the feature 
   */
  public Data getQuestionContext() {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_questionContext == null)
      jcasType.jcas.throwFeatMissing("questionContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    return (Data)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_questionContext)));}
    
  /** setter for questionContext - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setQuestionContext(Data v) {
    if (AnalyzedAnswerChoice_Type.featOkTst && ((AnalyzedAnswerChoice_Type)jcasType).casFeat_questionContext == null)
      jcasType.jcas.throwFeatMissing("questionContext", "edu.cmu.lti.ntcir.qalab.types.AnalyzedAnswerChoice");
    jcasType.ll_cas.ll_setRefValue(addr, ((AnalyzedAnswerChoice_Type)jcasType).casFeatCode_questionContext, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    