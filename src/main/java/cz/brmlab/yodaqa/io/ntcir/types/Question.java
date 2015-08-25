

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
public class Question extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Question.class);
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
  protected Question() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Question(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Question(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Question(JCas jcas, int begin, int end) {
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
  //* Feature: id

  /** getter for id - gets 
   * @generated
   * @return value of the feature 
   */
  public String getId() {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.Question");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Question_Type)jcasType).casFeatCode_id);}
    
  /** setter for id - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setId(String v) {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.Question");
    jcasType.ll_cas.ll_setStringValue(addr, ((Question_Type)jcasType).casFeatCode_id, v);}    
   
    
  //*--------------*
  //* Feature: contextData

  /** getter for contextData - gets 
   * @generated
   * @return value of the feature 
   */
  public Data getContextData() {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_contextData == null)
      jcasType.jcas.throwFeatMissing("contextData", "edu.cmu.lti.ntcir.qalab.types.Question");
    return (Data)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Question_Type)jcasType).casFeatCode_contextData)));}
    
  /** setter for contextData - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setContextData(Data v) {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_contextData == null)
      jcasType.jcas.throwFeatMissing("contextData", "edu.cmu.lti.ntcir.qalab.types.Question");
    jcasType.ll_cas.ll_setRefValue(addr, ((Question_Type)jcasType).casFeatCode_contextData, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: setinstruction

  /** getter for setinstruction - gets 
   * @generated
   * @return value of the feature 
   */
  public SetInstruction getSetinstruction() {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_setinstruction == null)
      jcasType.jcas.throwFeatMissing("setinstruction", "edu.cmu.lti.ntcir.qalab.types.Question");
    return (SetInstruction)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Question_Type)jcasType).casFeatCode_setinstruction)));}
    
  /** setter for setinstruction - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSetinstruction(SetInstruction v) {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_setinstruction == null)
      jcasType.jcas.throwFeatMissing("setinstruction", "edu.cmu.lti.ntcir.qalab.types.Question");
    jcasType.ll_cas.ll_setRefValue(addr, ((Question_Type)jcasType).casFeatCode_setinstruction, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: qdataList

  /** getter for qdataList - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getQdataList() {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_qdataList == null)
      jcasType.jcas.throwFeatMissing("qdataList", "edu.cmu.lti.ntcir.qalab.types.Question");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Question_Type)jcasType).casFeatCode_qdataList)));}
    
  /** setter for qdataList - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setQdataList(FSList v) {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_qdataList == null)
      jcasType.jcas.throwFeatMissing("qdataList", "edu.cmu.lti.ntcir.qalab.types.Question");
    jcasType.ll_cas.ll_setRefValue(addr, ((Question_Type)jcasType).casFeatCode_qdataList, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: questionType

  /** getter for questionType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getQuestionType() {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_questionType == null)
      jcasType.jcas.throwFeatMissing("questionType", "edu.cmu.lti.ntcir.qalab.types.Question");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Question_Type)jcasType).casFeatCode_questionType);}
    
  /** setter for questionType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setQuestionType(String v) {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_questionType == null)
      jcasType.jcas.throwFeatMissing("questionType", "edu.cmu.lti.ntcir.qalab.types.Question");
    jcasType.ll_cas.ll_setStringValue(addr, ((Question_Type)jcasType).casFeatCode_questionType, v);}    
   
    
  //*--------------*
  //* Feature: knowledgeType

  /** getter for knowledgeType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getKnowledgeType() {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_knowledgeType == null)
      jcasType.jcas.throwFeatMissing("knowledgeType", "edu.cmu.lti.ntcir.qalab.types.Question");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Question_Type)jcasType).casFeatCode_knowledgeType);}
    
  /** setter for knowledgeType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setKnowledgeType(String v) {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_knowledgeType == null)
      jcasType.jcas.throwFeatMissing("knowledgeType", "edu.cmu.lti.ntcir.qalab.types.Question");
    jcasType.ll_cas.ll_setStringValue(addr, ((Question_Type)jcasType).casFeatCode_knowledgeType, v);}    
   
    
  //*--------------*
  //* Feature: instruction

  /** getter for instruction - gets 
   * @generated
   * @return value of the feature 
   */
  public Instruction getInstruction() {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_instruction == null)
      jcasType.jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.Question");
    return (Instruction)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Question_Type)jcasType).casFeatCode_instruction)));}
    
  /** setter for instruction - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setInstruction(Instruction v) {
    if (Question_Type.featOkTst && ((Question_Type)jcasType).casFeat_instruction == null)
      jcasType.jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.Question");
    jcasType.ll_cas.ll_setRefValue(addr, ((Question_Type)jcasType).casFeatCode_instruction, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    