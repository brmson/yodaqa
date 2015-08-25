

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
public class Refs extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Refs.class);
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
  protected Refs() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Refs(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Refs(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Refs(JCas jcas, int begin, int end) {
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
  //* Feature: label

  /** getter for label - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLabel() {
    if (Refs_Type.featOkTst && ((Refs_Type)jcasType).casFeat_label == null)
      jcasType.jcas.throwFeatMissing("label", "edu.cmu.lti.ntcir.qalab.types.Refs");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Refs_Type)jcasType).casFeatCode_label);}
    
  /** setter for label - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLabel(String v) {
    if (Refs_Type.featOkTst && ((Refs_Type)jcasType).casFeat_label == null)
      jcasType.jcas.throwFeatMissing("label", "edu.cmu.lti.ntcir.qalab.types.Refs");
    jcasType.ll_cas.ll_setStringValue(addr, ((Refs_Type)jcasType).casFeatCode_label, v);}    
   
    
  //*--------------*
  //* Feature: text

  /** getter for text - gets 
   * @generated
   * @return value of the feature 
   */
  public String getText() {
    if (Refs_Type.featOkTst && ((Refs_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.Refs");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Refs_Type)jcasType).casFeatCode_text);}
    
  /** setter for text - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setText(String v) {
    if (Refs_Type.featOkTst && ((Refs_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.Refs");
    jcasType.ll_cas.ll_setStringValue(addr, ((Refs_Type)jcasType).casFeatCode_text, v);}    
   
    
  //*--------------*
  //* Feature: id

  /** getter for id - gets 
   * @generated
   * @return value of the feature 
   */
  public String getId() {
    if (Refs_Type.featOkTst && ((Refs_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.Refs");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Refs_Type)jcasType).casFeatCode_id);}
    
  /** setter for id - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setId(String v) {
    if (Refs_Type.featOkTst && ((Refs_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.Refs");
    jcasType.ll_cas.ll_setStringValue(addr, ((Refs_Type)jcasType).casFeatCode_id, v);}    
   
    
  //*--------------*
  //* Feature: target

  /** getter for target - gets The target that this reference referring to
   * @generated
   * @return value of the feature 
   */
  public RefTarget getTarget() {
    if (Refs_Type.featOkTst && ((Refs_Type)jcasType).casFeat_target == null)
      jcasType.jcas.throwFeatMissing("target", "edu.cmu.lti.ntcir.qalab.types.Refs");
    return (RefTarget)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Refs_Type)jcasType).casFeatCode_target)));}
    
  /** setter for target - sets The target that this reference referring to 
   * @generated
   * @param v value to set into the feature 
   */
  public void setTarget(RefTarget v) {
    if (Refs_Type.featOkTst && ((Refs_Type)jcasType).casFeat_target == null)
      jcasType.jcas.throwFeatMissing("target", "edu.cmu.lti.ntcir.qalab.types.Refs");
    jcasType.ll_cas.ll_setRefValue(addr, ((Refs_Type)jcasType).casFeatCode_target, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    