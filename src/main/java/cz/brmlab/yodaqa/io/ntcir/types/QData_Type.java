
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
public class QData_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (QData_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = QData_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new QData(addr, QData_Type.this);
  			   QData_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new QData(addr, QData_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = QData.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.ntcir.qalab.types.QData");
 
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
      jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.QData");
    return ll_cas.ll_getStringValue(addr, casFeatCode_text);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setText(int addr, String v) {
        if (featOkTst && casFeat_text == null)
      jcas.throwFeatMissing("text", "edu.cmu.lti.ntcir.qalab.types.QData");
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
      jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.QData");
    return ll_cas.ll_getStringValue(addr, casFeatCode_id);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setId(int addr, String v) {
        if (featOkTst && casFeat_id == null)
      jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.QData");
    ll_cas.ll_setStringValue(addr, casFeatCode_id, v);}
    
  
 
  /** @generated */
  final Feature casFeat_listItems;
  /** @generated */
  final int     casFeatCode_listItems;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getListItems(int addr) {
        if (featOkTst && casFeat_listItems == null)
      jcas.throwFeatMissing("listItems", "edu.cmu.lti.ntcir.qalab.types.QData");
    return ll_cas.ll_getRefValue(addr, casFeatCode_listItems);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setListItems(int addr, int v) {
        if (featOkTst && casFeat_listItems == null)
      jcas.throwFeatMissing("listItems", "edu.cmu.lti.ntcir.qalab.types.QData");
    ll_cas.ll_setRefValue(addr, casFeatCode_listItems, v);}
    
  
 
  /** @generated */
  final Feature casFeat_gaps;
  /** @generated */
  final int     casFeatCode_gaps;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getGaps(int addr) {
        if (featOkTst && casFeat_gaps == null)
      jcas.throwFeatMissing("gaps", "edu.cmu.lti.ntcir.qalab.types.QData");
    return ll_cas.ll_getRefValue(addr, casFeatCode_gaps);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setGaps(int addr, int v) {
        if (featOkTst && casFeat_gaps == null)
      jcas.throwFeatMissing("gaps", "edu.cmu.lti.ntcir.qalab.types.QData");
    ll_cas.ll_setRefValue(addr, casFeatCode_gaps, v);}
    
  
 
  /** @generated */
  final Feature casFeat_refs;
  /** @generated */
  final int     casFeatCode_refs;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getRefs(int addr) {
        if (featOkTst && casFeat_refs == null)
      jcas.throwFeatMissing("refs", "edu.cmu.lti.ntcir.qalab.types.QData");
    return ll_cas.ll_getRefValue(addr, casFeatCode_refs);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRefs(int addr, int v) {
        if (featOkTst && casFeat_refs == null)
      jcas.throwFeatMissing("refs", "edu.cmu.lti.ntcir.qalab.types.QData");
    ll_cas.ll_setRefValue(addr, casFeatCode_refs, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public QData_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_text = jcas.getRequiredFeatureDE(casType, "text", "uima.cas.String", featOkTst);
    casFeatCode_text  = (null == casFeat_text) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_text).getCode();

 
    casFeat_id = jcas.getRequiredFeatureDE(casType, "id", "uima.cas.String", featOkTst);
    casFeatCode_id  = (null == casFeat_id) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_id).getCode();

 
    casFeat_listItems = jcas.getRequiredFeatureDE(casType, "listItems", "uima.cas.FSList", featOkTst);
    casFeatCode_listItems  = (null == casFeat_listItems) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_listItems).getCode();

 
    casFeat_gaps = jcas.getRequiredFeatureDE(casType, "gaps", "uima.cas.FSList", featOkTst);
    casFeatCode_gaps  = (null == casFeat_gaps) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_gaps).getCode();

 
    casFeat_refs = jcas.getRequiredFeatureDE(casType, "refs", "uima.cas.FSList", featOkTst);
    casFeatCode_refs  = (null == casFeat_refs) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_refs).getCode();

  }
}



    