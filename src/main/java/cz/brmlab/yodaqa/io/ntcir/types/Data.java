

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
public class Data extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Data.class);
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
  protected Data() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Data(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Data(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Data(JCas jcas, int begin, int end) {
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
    if (Data_Type.featOkTst && ((Data_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.Data");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Data_Type)jcasType).casFeatCode_text);}
    
  /** setter for text - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setText(String v) {
    if (Data_Type.featOkTst && ((Data_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.Data");
    jcasType.ll_cas.ll_setStringValue(addr, ((Data_Type)jcasType).casFeatCode_text, v);}    
   
    
  //*--------------*
  //* Feature: id

  /** getter for id - gets 
   * @generated
   * @return value of the feature 
   */
  public String getId() {
    if (Data_Type.featOkTst && ((Data_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.Data");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Data_Type)jcasType).casFeatCode_id);}
    
  /** setter for id - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setId(String v) {
    if (Data_Type.featOkTst && ((Data_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.Data");
    jcasType.ll_cas.ll_setStringValue(addr, ((Data_Type)jcasType).casFeatCode_id, v);}    
   
    
  //*--------------*
  //* Feature: underlinedList

  /** getter for underlinedList - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getUnderlinedList() {
    if (Data_Type.featOkTst && ((Data_Type)jcasType).casFeat_underlinedList == null)
      jcasType.jcas.throwFeatMissing("underlinedList", "edu.cmu.lti.ntcir.qalab.types.Data");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Data_Type)jcasType).casFeatCode_underlinedList)));}
    
  /** setter for underlinedList - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setUnderlinedList(FSList v) {
    if (Data_Type.featOkTst && ((Data_Type)jcasType).casFeat_underlinedList == null)
      jcasType.jcas.throwFeatMissing("underlinedList", "edu.cmu.lti.ntcir.qalab.types.Data");
    jcasType.ll_cas.ll_setRefValue(addr, ((Data_Type)jcasType).casFeatCode_underlinedList, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: gapList

  /** getter for gapList - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getGapList() {
    if (Data_Type.featOkTst && ((Data_Type)jcasType).casFeat_gapList == null)
      jcasType.jcas.throwFeatMissing("gapList", "edu.cmu.lti.ntcir.qalab.types.Data");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Data_Type)jcasType).casFeatCode_gapList)));}
    
  /** setter for gapList - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setGapList(FSList v) {
    if (Data_Type.featOkTst && ((Data_Type)jcasType).casFeat_gapList == null)
      jcasType.jcas.throwFeatMissing("gapList", "edu.cmu.lti.ntcir.qalab.types.Data");
    jcasType.ll_cas.ll_setRefValue(addr, ((Data_Type)jcasType).casFeatCode_gapList, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    