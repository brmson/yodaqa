
/* First created by JCasGen Mon Aug 04 11:44:10 EDT 2014 */
package cz.brmlab.yodaqa.io.ntcir.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Mon Aug 04 11:44:10 EDT 2014
 * @generated */
public class AnswerChoice_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (AnswerChoice_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = AnswerChoice_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new AnswerChoice(addr, AnswerChoice_Type.this);
  			   AnswerChoice_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new AnswerChoice(addr, AnswerChoice_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = AnswerChoice.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
 
  /** @generated */
  final Feature casFeat_id;
  /** @generated */
  final int     casFeatCode_id;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getId(int addr) {
        if (featOkTst && casFeat_id == null)
      jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return ll_cas.ll_getStringValue(addr, casFeatCode_id);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setId(int addr, String v) {
        if (featOkTst && casFeat_id == null)
      jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    ll_cas.ll_setStringValue(addr, casFeatCode_id, v);}
    
  
 
  /** @generated */
  final Feature casFeat_text;
  /** @generated */
  final int     casFeatCode_text;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getText(int addr) {
        if (featOkTst && casFeat_text == null)
      jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return ll_cas.ll_getStringValue(addr, casFeatCode_text);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setText(int addr, String v) {
        if (featOkTst && casFeat_text == null)
      jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    ll_cas.ll_setStringValue(addr, casFeatCode_text, v);}
    
  
 
  /** @generated */
  final Feature casFeat_isCorrect;
  /** @generated */
  final int     casFeatCode_isCorrect;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getIsCorrect(int addr) {
        if (featOkTst && casFeat_isCorrect == null)
      jcas.throwFeatMissing("isCorrect", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isCorrect);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIsCorrect(int addr, boolean v) {
        if (featOkTst && casFeat_isCorrect == null)
      jcas.throwFeatMissing("isCorrect", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isCorrect, v);}
    
  
 
  /** @generated */
  final Feature casFeat_isSelected;
  /** @generated */
  final int     casFeatCode_isSelected;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getIsSelected(int addr) {
        if (featOkTst && casFeat_isSelected == null)
      jcas.throwFeatMissing("isSelected", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isSelected);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIsSelected(int addr, boolean v) {
        if (featOkTst && casFeat_isSelected == null)
      jcas.throwFeatMissing("isSelected", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isSelected, v);}
    
  
 
  /** @generated */
  final Feature casFeat_refList;
  /** @generated */
  final int     casFeatCode_refList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getRefList(int addr) {
        if (featOkTst && casFeat_refList == null)
      jcas.throwFeatMissing("refList", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return ll_cas.ll_getRefValue(addr, casFeatCode_refList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRefList(int addr, int v) {
        if (featOkTst && casFeat_refList == null)
      jcas.throwFeatMissing("refList", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    ll_cas.ll_setRefValue(addr, casFeatCode_refList, v);}
    
  
 
  /** @generated */
  final Feature casFeat_choiceNum;
  /** @generated */
  final int     casFeatCode_choiceNum;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getChoiceNum(int addr) {
        if (featOkTst && casFeat_choiceNum == null)
      jcas.throwFeatMissing("choiceNum", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    return ll_cas.ll_getRefValue(addr, casFeatCode_choiceNum);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setChoiceNum(int addr, int v) {
        if (featOkTst && casFeat_choiceNum == null)
      jcas.throwFeatMissing("choiceNum", "edu.cmu.lti.ntcir.qalab.types.AnswerChoice");
    ll_cas.ll_setRefValue(addr, casFeatCode_choiceNum, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public AnswerChoice_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_id = jcas.getRequiredFeatureDE(casType, "id", "uima.cas.String", featOkTst);
    casFeatCode_id  = (null == casFeat_id) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_id).getCode();

 
    casFeat_text = jcas.getRequiredFeatureDE(casType, "text", "uima.cas.String", featOkTst);
    casFeatCode_text  = (null == casFeat_text) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_text).getCode();

 
    casFeat_isCorrect = jcas.getRequiredFeatureDE(casType, "isCorrect", "uima.cas.Boolean", featOkTst);
    casFeatCode_isCorrect  = (null == casFeat_isCorrect) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isCorrect).getCode();

 
    casFeat_isSelected = jcas.getRequiredFeatureDE(casType, "isSelected", "uima.cas.Boolean", featOkTst);
    casFeatCode_isSelected  = (null == casFeat_isSelected) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isSelected).getCode();

 
    casFeat_refList = jcas.getRequiredFeatureDE(casType, "refList", "uima.cas.FSList", featOkTst);
    casFeatCode_refList  = (null == casFeat_refList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_refList).getCode();

 
    casFeat_choiceNum = jcas.getRequiredFeatureDE(casType, "choiceNum", "edu.cmu.lti.ntcir.qalab.types.ChoiceNumber", featOkTst);
    casFeatCode_choiceNum  = (null == casFeat_choiceNum) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_choiceNum).getCode();

  }
}



    