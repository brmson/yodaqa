

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
public class Assertion extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Assertion.class);
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
  protected Assertion() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Assertion(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Assertion(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Assertion(JCas jcas, int begin, int end) {
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
  //* Feature: text

  /** getter for text - gets 
   * @generated
   * @return value of the feature 
   */
  public String getText() {
    if (Assertion_Type.featOkTst && ((Assertion_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Assertion_Type)jcasType).casFeatCode_text);}
    
  /** setter for text - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setText(String v) {
    if (Assertion_Type.featOkTst && ((Assertion_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    jcasType.ll_cas.ll_setStringValue(addr, ((Assertion_Type)jcasType).casFeatCode_text, v);}    
   
    
  //*--------------*
  //* Feature: isAffirmative

  /** getter for isAffirmative - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getIsAffirmative() {
    if (Assertion_Type.featOkTst && ((Assertion_Type)jcasType).casFeat_isAffirmative == null)
      jcasType.jcas.throwFeatMissing("isAffirmative", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((Assertion_Type)jcasType).casFeatCode_isAffirmative);}
    
  /** setter for isAffirmative - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setIsAffirmative(boolean v) {
    if (Assertion_Type.featOkTst && ((Assertion_Type)jcasType).casFeat_isAffirmative == null)
      jcasType.jcas.throwFeatMissing("isAffirmative", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((Assertion_Type)jcasType).casFeatCode_isAffirmative, v);}    
   
    
  //*--------------*
  //* Feature: assertScoreList

  /** getter for assertScoreList - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAssertScoreList() {
    if (Assertion_Type.featOkTst && ((Assertion_Type)jcasType).casFeat_assertScoreList == null)
      jcasType.jcas.throwFeatMissing("assertScoreList", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Assertion_Type)jcasType).casFeatCode_assertScoreList)));}
    
  /** setter for assertScoreList - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAssertScoreList(FSList v) {
    if (Assertion_Type.featOkTst && ((Assertion_Type)jcasType).casFeat_assertScoreList == null)
      jcasType.jcas.throwFeatMissing("assertScoreList", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    jcasType.ll_cas.ll_setRefValue(addr, ((Assertion_Type)jcasType).casFeatCode_assertScoreList, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    