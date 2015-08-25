
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
public class Data_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Data_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Data_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Data(addr, Data_Type.this);
  			   Data_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Data(addr, Data_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Data.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.ntcir.qalab.types.Data");
 
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
      jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.Data");
    return ll_cas.ll_getStringValue(addr, casFeatCode_text);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setText(int addr, String v) {
        if (featOkTst && casFeat_text == null)
      jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.Data");
    ll_cas.ll_setStringValue(addr, casFeatCode_text, v);}
    
  
 
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
      jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.Data");
    return ll_cas.ll_getStringValue(addr, casFeatCode_id);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setId(int addr, String v) {
        if (featOkTst && casFeat_id == null)
      jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.Data");
    ll_cas.ll_setStringValue(addr, casFeatCode_id, v);}
    
  
 
  /** @generated */
  final Feature casFeat_underlinedList;
  /** @generated */
  final int     casFeatCode_underlinedList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getUnderlinedList(int addr) {
        if (featOkTst && casFeat_underlinedList == null)
      jcas.throwFeatMissing("underlinedList", "edu.cmu.lti.ntcir.qalab.types.Data");
    return ll_cas.ll_getRefValue(addr, casFeatCode_underlinedList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setUnderlinedList(int addr, int v) {
        if (featOkTst && casFeat_underlinedList == null)
      jcas.throwFeatMissing("underlinedList", "edu.cmu.lti.ntcir.qalab.types.Data");
    ll_cas.ll_setRefValue(addr, casFeatCode_underlinedList, v);}
    
  
 
  /** @generated */
  final Feature casFeat_gapList;
  /** @generated */
  final int     casFeatCode_gapList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getGapList(int addr) {
        if (featOkTst && casFeat_gapList == null)
      jcas.throwFeatMissing("gapList", "edu.cmu.lti.ntcir.qalab.types.Data");
    return ll_cas.ll_getRefValue(addr, casFeatCode_gapList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setGapList(int addr, int v) {
        if (featOkTst && casFeat_gapList == null)
      jcas.throwFeatMissing("gapList", "edu.cmu.lti.ntcir.qalab.types.Data");
    ll_cas.ll_setRefValue(addr, casFeatCode_gapList, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Data_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_text = jcas.getRequiredFeatureDE(casType, "text", "uima.cas.String", featOkTst);
    casFeatCode_text  = (null == casFeat_text) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_text).getCode();

 
    casFeat_id = jcas.getRequiredFeatureDE(casType, "id", "uima.cas.String", featOkTst);
    casFeatCode_id  = (null == casFeat_id) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_id).getCode();

 
    casFeat_underlinedList = jcas.getRequiredFeatureDE(casType, "underlinedList", "uima.cas.FSList", featOkTst);
    casFeatCode_underlinedList  = (null == casFeat_underlinedList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_underlinedList).getCode();

 
    casFeat_gapList = jcas.getRequiredFeatureDE(casType, "gapList", "uima.cas.FSList", featOkTst);
    casFeatCode_gapList  = (null == casFeat_gapList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_gapList).getCode();

  }
}



    