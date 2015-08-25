
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
public class Assertion_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Assertion_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Assertion_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Assertion(addr, Assertion_Type.this);
  			   Assertion_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Assertion(addr, Assertion_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Assertion.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.ntcir.qalab.types.Assertion");
 
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
      jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    return ll_cas.ll_getStringValue(addr, casFeatCode_text);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setText(int addr, String v) {
        if (featOkTst && casFeat_text == null)
      jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    ll_cas.ll_setStringValue(addr, casFeatCode_text, v);}
    
  
 
  /** @generated */
  final Feature casFeat_isAffirmative;
  /** @generated */
  final int     casFeatCode_isAffirmative;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getIsAffirmative(int addr) {
        if (featOkTst && casFeat_isAffirmative == null)
      jcas.throwFeatMissing("isAffirmative", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isAffirmative);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIsAffirmative(int addr, boolean v) {
        if (featOkTst && casFeat_isAffirmative == null)
      jcas.throwFeatMissing("isAffirmative", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isAffirmative, v);}
    
  
 
  /** @generated */
  final Feature casFeat_assertScoreList;
  /** @generated */
  final int     casFeatCode_assertScoreList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAssertScoreList(int addr) {
        if (featOkTst && casFeat_assertScoreList == null)
      jcas.throwFeatMissing("assertScoreList", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    return ll_cas.ll_getRefValue(addr, casFeatCode_assertScoreList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAssertScoreList(int addr, int v) {
        if (featOkTst && casFeat_assertScoreList == null)
      jcas.throwFeatMissing("assertScoreList", "edu.cmu.lti.ntcir.qalab.types.Assertion");
    ll_cas.ll_setRefValue(addr, casFeatCode_assertScoreList, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Assertion_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_text = jcas.getRequiredFeatureDE(casType, "text", "uima.cas.String", featOkTst);
    casFeatCode_text  = (null == casFeat_text) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_text).getCode();

 
    casFeat_isAffirmative = jcas.getRequiredFeatureDE(casType, "isAffirmative", "uima.cas.Boolean", featOkTst);
    casFeatCode_isAffirmative  = (null == casFeat_isAffirmative) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isAffirmative).getCode();

 
    casFeat_assertScoreList = jcas.getRequiredFeatureDE(casType, "assertScoreList", "uima.cas.FSList", featOkTst);
    casFeatCode_assertScoreList  = (null == casFeat_assertScoreList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_assertScoreList).getCode();

  }
}



    