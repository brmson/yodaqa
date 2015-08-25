
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
public class TestDocument_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (TestDocument_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = TestDocument_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new TestDocument(addr, TestDocument_Type.this);
  			   TestDocument_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new TestDocument(addr, TestDocument_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = TestDocument.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.ntcir.qalab.types.TestDocument");
 
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
      jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    return ll_cas.ll_getStringValue(addr, casFeatCode_id);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setId(int addr, String v) {
        if (featOkTst && casFeat_id == null)
      jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    ll_cas.ll_setStringValue(addr, casFeatCode_id, v);}
    
  
 
  /** @generated */
  final Feature casFeat_instruction;
  /** @generated */
  final int     casFeatCode_instruction;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getInstruction(int addr) {
        if (featOkTst && casFeat_instruction == null)
      jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    return ll_cas.ll_getRefValue(addr, casFeatCode_instruction);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setInstruction(int addr, int v) {
        if (featOkTst && casFeat_instruction == null)
      jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    ll_cas.ll_setRefValue(addr, casFeatCode_instruction, v);}
    
  
 
  /** @generated */
  final Feature casFeat_QAList;
  /** @generated */
  final int     casFeatCode_QAList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getQAList(int addr) {
        if (featOkTst && casFeat_QAList == null)
      jcas.throwFeatMissing("QAList", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    return ll_cas.ll_getRefValue(addr, casFeatCode_QAList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setQAList(int addr, int v) {
        if (featOkTst && casFeat_QAList == null)
      jcas.throwFeatMissing("QAList", "edu.cmu.lti.ntcir.qalab.types.TestDocument");
    ll_cas.ll_setRefValue(addr, casFeatCode_QAList, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public TestDocument_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_id = jcas.getRequiredFeatureDE(casType, "id", "uima.cas.String", featOkTst);
    casFeatCode_id  = (null == casFeat_id) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_id).getCode();

 
    casFeat_instruction = jcas.getRequiredFeatureDE(casType, "instruction", "edu.cmu.lti.ntcir.qalab.types.SetInstruction", featOkTst);
    casFeatCode_instruction  = (null == casFeat_instruction) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_instruction).getCode();

 
    casFeat_QAList = jcas.getRequiredFeatureDE(casType, "QAList", "uima.cas.FSList", featOkTst);
    casFeatCode_QAList  = (null == casFeat_QAList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_QAList).getCode();

  }
}



    