

/* First created by JCasGen Mon Aug 04 11:44:10 EDT 2014 */
package cz.brmlab.yodaqa.io.ntcir.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Mon Aug 04 11:44:10 EDT 2014
 * XML source: /home/diwang/Dropbox/oaqa-workspace/ntcir-qalab-cmu-baseline/src/main/resources/WorldHistoryTypesDescriptor.xml
 * @generated */
public class SetInstruction extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SetInstruction.class);
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
  protected SetInstruction() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SetInstruction(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SetInstruction(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public SetInstruction(JCas jcas, int begin, int end) {
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
    if (SetInstruction_Type.featOkTst && ((SetInstruction_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.SetInstruction");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SetInstruction_Type)jcasType).casFeatCode_text);}
    
  /** setter for text - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setText(String v) {
    if (SetInstruction_Type.featOkTst && ((SetInstruction_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.SetInstruction");
    jcasType.ll_cas.ll_setStringValue(addr, ((SetInstruction_Type)jcasType).casFeatCode_text, v);}    
   
    
  //*--------------*
  //* Feature: topic

  /** getter for topic - gets 
   * @generated
   * @return value of the feature 
   */
  public String getTopic() {
    if (SetInstruction_Type.featOkTst && ((SetInstruction_Type)jcasType).casFeat_topic == null)
      jcasType.jcas.throwFeatMissing("topic", "edu.cmu.lti.ntcir.qalab.types.SetInstruction");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SetInstruction_Type)jcasType).casFeatCode_topic);}
    
  /** setter for topic - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTopic(String v) {
    if (SetInstruction_Type.featOkTst && ((SetInstruction_Type)jcasType).casFeat_topic == null)
      jcasType.jcas.throwFeatMissing("topic", "edu.cmu.lti.ntcir.qalab.types.SetInstruction");
    jcasType.ll_cas.ll_setStringValue(addr, ((SetInstruction_Type)jcasType).casFeatCode_topic, v);}    
  }

    