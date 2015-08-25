

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
public class QData extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(QData.class);
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
  protected QData() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public QData(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public QData(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public QData(JCas jcas, int begin, int end) {
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
    if (QData_Type.featOkTst && ((QData_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.QData");
    return jcasType.ll_cas.ll_getStringValue(addr, ((QData_Type)jcasType).casFeatCode_text);}
    
  /** setter for text - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setText(String v) {
    if (QData_Type.featOkTst && ((QData_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.QData");
    jcasType.ll_cas.ll_setStringValue(addr, ((QData_Type)jcasType).casFeatCode_text, v);}    
   
    
  //*--------------*
  //* Feature: id

  /** getter for id - gets 
   * @generated
   * @return value of the feature 
   */
  public String getId() {
    if (QData_Type.featOkTst && ((QData_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.QData");
    return jcasType.ll_cas.ll_getStringValue(addr, ((QData_Type)jcasType).casFeatCode_id);}
    
  /** setter for id - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setId(String v) {
    if (QData_Type.featOkTst && ((QData_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.QData");
    jcasType.ll_cas.ll_setStringValue(addr, ((QData_Type)jcasType).casFeatCode_id, v);}    
   
    
  //*--------------*
  //* Feature: listItems

  /** getter for listItems - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getListItems() {
    if (QData_Type.featOkTst && ((QData_Type)jcasType).casFeat_listItems == null)
      jcasType.jcas.throwFeatMissing("listItems", "edu.cmu.lti.ntcir.qalab.types.QData");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((QData_Type)jcasType).casFeatCode_listItems)));}
    
  /** setter for listItems - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setListItems(FSList v) {
    if (QData_Type.featOkTst && ((QData_Type)jcasType).casFeat_listItems == null)
      jcasType.jcas.throwFeatMissing("listItems", "edu.cmu.lti.ntcir.qalab.types.QData");
    jcasType.ll_cas.ll_setRefValue(addr, ((QData_Type)jcasType).casFeatCode_listItems, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: gaps

  /** getter for gaps - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getGaps() {
    if (QData_Type.featOkTst && ((QData_Type)jcasType).casFeat_gaps == null)
      jcasType.jcas.throwFeatMissing("gaps", "edu.cmu.lti.ntcir.qalab.types.QData");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((QData_Type)jcasType).casFeatCode_gaps)));}
    
  /** setter for gaps - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setGaps(FSList v) {
    if (QData_Type.featOkTst && ((QData_Type)jcasType).casFeat_gaps == null)
      jcasType.jcas.throwFeatMissing("gaps", "edu.cmu.lti.ntcir.qalab.types.QData");
    jcasType.ll_cas.ll_setRefValue(addr, ((QData_Type)jcasType).casFeatCode_gaps, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: refs

  /** getter for refs - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getRefs() {
    if (QData_Type.featOkTst && ((QData_Type)jcasType).casFeat_refs == null)
      jcasType.jcas.throwFeatMissing("refs", "edu.cmu.lti.ntcir.qalab.types.QData");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((QData_Type)jcasType).casFeatCode_refs)));}
    
  /** setter for refs - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setRefs(FSList v) {
    if (QData_Type.featOkTst && ((QData_Type)jcasType).casFeat_refs == null)
      jcasType.jcas.throwFeatMissing("refs", "edu.cmu.lti.ntcir.qalab.types.QData");
    jcasType.ll_cas.ll_setRefValue(addr, ((QData_Type)jcasType).casFeatCode_refs, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    