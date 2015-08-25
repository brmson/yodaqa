

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
public class QuestionAnswerSet extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(QuestionAnswerSet.class);
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
  protected QuestionAnswerSet() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public QuestionAnswerSet(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public QuestionAnswerSet(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public QuestionAnswerSet(JCas jcas, int begin, int end) {
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
  //* Feature: question

  /** getter for question - gets 
   * @generated
   * @return value of the feature 
   */
  public Question getQuestion() {
    if (QuestionAnswerSet_Type.featOkTst && ((QuestionAnswerSet_Type)jcasType).casFeat_question == null)
      jcasType.jcas.throwFeatMissing("question", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    return (Question)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((QuestionAnswerSet_Type)jcasType).casFeatCode_question)));}
    
  /** setter for question - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setQuestion(Question v) {
    if (QuestionAnswerSet_Type.featOkTst && ((QuestionAnswerSet_Type)jcasType).casFeat_question == null)
      jcasType.jcas.throwFeatMissing("question", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    jcasType.ll_cas.ll_setRefValue(addr, ((QuestionAnswerSet_Type)jcasType).casFeatCode_question, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: answerChoiceList

  /** getter for answerChoiceList - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAnswerChoiceList() {
    if (QuestionAnswerSet_Type.featOkTst && ((QuestionAnswerSet_Type)jcasType).casFeat_answerChoiceList == null)
      jcasType.jcas.throwFeatMissing("answerChoiceList", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((QuestionAnswerSet_Type)jcasType).casFeatCode_answerChoiceList)));}
    
  /** setter for answerChoiceList - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAnswerChoiceList(FSList v) {
    if (QuestionAnswerSet_Type.featOkTst && ((QuestionAnswerSet_Type)jcasType).casFeat_answerChoiceList == null)
      jcasType.jcas.throwFeatMissing("answerChoiceList", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    jcasType.ll_cas.ll_setRefValue(addr, ((QuestionAnswerSet_Type)jcasType).casFeatCode_answerChoiceList, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: analyzedAnswerChoiceList

  /** getter for analyzedAnswerChoiceList - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAnalyzedAnswerChoiceList() {
    if (QuestionAnswerSet_Type.featOkTst && ((QuestionAnswerSet_Type)jcasType).casFeat_analyzedAnswerChoiceList == null)
      jcasType.jcas.throwFeatMissing("analyzedAnswerChoiceList", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((QuestionAnswerSet_Type)jcasType).casFeatCode_analyzedAnswerChoiceList)));}
    
  /** setter for analyzedAnswerChoiceList - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAnalyzedAnswerChoiceList(FSList v) {
    if (QuestionAnswerSet_Type.featOkTst && ((QuestionAnswerSet_Type)jcasType).casFeat_analyzedAnswerChoiceList == null)
      jcasType.jcas.throwFeatMissing("analyzedAnswerChoiceList", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    jcasType.ll_cas.ll_setRefValue(addr, ((QuestionAnswerSet_Type)jcasType).casFeatCode_analyzedAnswerChoiceList, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    