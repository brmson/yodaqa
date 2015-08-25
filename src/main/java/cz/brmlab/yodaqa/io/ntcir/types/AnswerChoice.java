

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
public class AnswerChoice extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(AnswerChoice.class);
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
  protected AnswerChoice() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public AnswerChoice(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public AnswerChoice(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public AnswerChoice(JCas jcas, int begin, int end) {
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
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_id);}
    
  /** setter for id - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setId(String v) {
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    jcasType.ll_cas.ll_setStringValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_id, v);}    
   
    
  //*--------------*
  //* Feature: text

  /** getter for text - gets 
   * @generated
   * @return value of the feature 
   */
  public String getText() {
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_text);}
    
  /** setter for text - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setText(String v) {
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    jcasType.ll_cas.ll_setStringValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_text, v);}    
   
    
  //*--------------*
  //* Feature: isCorrect

  /** getter for isCorrect - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getIsCorrect() {
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_isCorrect == null)
      jcasType.jcas.throwFeatMissing("isCorrect", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_isCorrect);}
    
  /** setter for isCorrect - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setIsCorrect(boolean v) {
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_isCorrect == null)
      jcasType.jcas.throwFeatMissing("isCorrect", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_isCorrect, v);}    
   
    
  //*--------------*
  //* Feature: isSelected

  /** getter for isSelected - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getIsSelected() {
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_isSelected == null)
      jcasType.jcas.throwFeatMissing("isSelected", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_isSelected);}
    
  /** setter for isSelected - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setIsSelected(boolean v) {
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_isSelected == null)
      jcasType.jcas.throwFeatMissing("isSelected", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_isSelected, v);}    
   
    
  //*--------------*
  //* Feature: refList

  /** getter for refList - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getRefList() {
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_refList == null)
      jcasType.jcas.throwFeatMissing("refList", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_refList)));}
    
  /** setter for refList - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setRefList(FSList v) {
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_refList == null)
      jcasType.jcas.throwFeatMissing("refList", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    jcasType.ll_cas.ll_setRefValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_refList, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: choiceNum

  /** getter for choiceNum - gets 
   * @generated
   * @return value of the feature 
   */
  public ChoiceNumber getChoiceNum() {
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_choiceNum == null)
      jcasType.jcas.throwFeatMissing("choiceNum", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return (ChoiceNumber)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_choiceNum)));}
    
  /** setter for choiceNum - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChoiceNum(ChoiceNumber v) {
    if (AnswerChoice_Type.featOkTst && ((AnswerChoice_Type)jcasType).casFeat_choiceNum == null)
      jcasType.jcas.throwFeatMissing("choiceNum", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    jcasType.ll_cas.ll_setRefValue(addr, ((AnswerChoice_Type)jcasType).casFeatCode_choiceNum, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    