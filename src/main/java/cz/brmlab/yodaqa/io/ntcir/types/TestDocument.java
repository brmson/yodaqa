

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
public class TestDocument extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(TestDocument.class);
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
  protected TestDocument() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public TestDocument(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public TestDocument(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public TestDocument(JCas jcas, int begin, int end) {
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
    if (TestDocument_Type.featOkTst && ((TestDocument_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    return jcasType.ll_cas.ll_getStringValue(addr, ((TestDocument_Type)jcasType).casFeatCode_id);}
    
  /** setter for id - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setId(String v) {
    if (TestDocument_Type.featOkTst && ((TestDocument_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    jcasType.ll_cas.ll_setStringValue(addr, ((TestDocument_Type)jcasType).casFeatCode_id, v);}    
   
    
  //*--------------*
  //* Feature: instruction

  /** getter for instruction - gets 
   * @generated
   * @return value of the feature 
   */
  public SetInstruction getInstruction() {
    if (TestDocument_Type.featOkTst && ((TestDocument_Type)jcasType).casFeat_instruction == null)
      jcasType.jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    return (SetInstruction)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((TestDocument_Type)jcasType).casFeatCode_instruction)));}
    
  /** setter for instruction - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setInstruction(SetInstruction v) {
    if (TestDocument_Type.featOkTst && ((TestDocument_Type)jcasType).casFeat_instruction == null)
      jcasType.jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    jcasType.ll_cas.ll_setRefValue(addr, ((TestDocument_Type)jcasType).casFeatCode_instruction, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: QAList

  /** getter for QAList - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getQAList() {
    if (TestDocument_Type.featOkTst && ((TestDocument_Type)jcasType).casFeat_QAList == null)
      jcasType.jcas.throwFeatMissing("QAList", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((TestDocument_Type)jcasType).casFeatCode_QAList)));}
    
  /** setter for QAList - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setQAList(FSList v) {
    if (TestDocument_Type.featOkTst && ((TestDocument_Type)jcasType).casFeat_QAList == null)
      jcasType.jcas.throwFeatMissing("QAList", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    jcasType.ll_cas.ll_setRefValue(addr, ((TestDocument_Type)jcasType).casFeatCode_QAList, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    