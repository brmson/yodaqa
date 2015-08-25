
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
public class Question_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Question_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Question_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Question(addr, Question_Type.this);
  			   Question_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Question(addr, Question_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Question.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.ntcir.qalab.types.Question");
 
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
      jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.Question");
    return ll_cas.ll_getStringValue(addr, casFeatCode_id);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setId(int addr, String v) {
        if (featOkTst && casFeat_id == null)
      jcas.throwFeatMissing("id", "edu.cmu.lti.ntcir.qalab.types.Question");
    ll_cas.ll_setStringValue(addr, casFeatCode_id, v);}
    
  
 
  /** @generated */
  final Feature casFeat_contextData;
  /** @generated */
  final int     casFeatCode_contextData;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getContextData(int addr) {
        if (featOkTst && casFeat_contextData == null)
      jcas.throwFeatMissing("contextData", "edu.cmu.lti.ntcir.qalab.types.Question");
    return ll_cas.ll_getRefValue(addr, casFeatCode_contextData);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setContextData(int addr, int v) {
        if (featOkTst && casFeat_contextData == null)
      jcas.throwFeatMissing("contextData", "edu.cmu.lti.ntcir.qalab.types.Question");
    ll_cas.ll_setRefValue(addr, casFeatCode_contextData, v);}
    
  
 
  /** @generated */
  final Feature casFeat_setinstruction;
  /** @generated */
  final int     casFeatCode_setinstruction;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getSetinstruction(int addr) {
        if (featOkTst && casFeat_setinstruction == null)
      jcas.throwFeatMissing("setinstruction", "edu.cmu.lti.ntcir.qalab.types.Question");
    return ll_cas.ll_getRefValue(addr, casFeatCode_setinstruction);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSetinstruction(int addr, int v) {
        if (featOkTst && casFeat_setinstruction == null)
      jcas.throwFeatMissing("setinstruction", "edu.cmu.lti.ntcir.qalab.types.Question");
    ll_cas.ll_setRefValue(addr, casFeatCode_setinstruction, v);}
    
  
 
  /** @generated */
  final Feature casFeat_qdataList;
  /** @generated */
  final int     casFeatCode_qdataList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getQdataList(int addr) {
        if (featOkTst && casFeat_qdataList == null)
      jcas.throwFeatMissing("qdataList", "edu.cmu.lti.ntcir.qalab.types.Question");
    return ll_cas.ll_getRefValue(addr, casFeatCode_qdataList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setQdataList(int addr, int v) {
        if (featOkTst && casFeat_qdataList == null)
      jcas.throwFeatMissing("qdataList", "edu.cmu.lti.ntcir.qalab.types.Question");
    ll_cas.ll_setRefValue(addr, casFeatCode_qdataList, v);}
    
  
 
  /** @generated */
  final Feature casFeat_questionType;
  /** @generated */
  final int     casFeatCode_questionType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getQuestionType(int addr) {
        if (featOkTst && casFeat_questionType == null)
      jcas.throwFeatMissing("questionType", "edu.cmu.lti.ntcir.qalab.types.Question");
    return ll_cas.ll_getStringValue(addr, casFeatCode_questionType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setQuestionType(int addr, String v) {
        if (featOkTst && casFeat_questionType == null)
      jcas.throwFeatMissing("questionType", "edu.cmu.lti.ntcir.qalab.types.Question");
    ll_cas.ll_setStringValue(addr, casFeatCode_questionType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_knowledgeType;
  /** @generated */
  final int     casFeatCode_knowledgeType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getKnowledgeType(int addr) {
        if (featOkTst && casFeat_knowledgeType == null)
      jcas.throwFeatMissing("knowledgeType", "edu.cmu.lti.ntcir.qalab.types.Question");
    return ll_cas.ll_getStringValue(addr, casFeatCode_knowledgeType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setKnowledgeType(int addr, String v) {
        if (featOkTst && casFeat_knowledgeType == null)
      jcas.throwFeatMissing("knowledgeType", "edu.cmu.lti.ntcir.qalab.types.Question");
    ll_cas.ll_setStringValue(addr, casFeatCode_knowledgeType, v);}
    
  
 
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
      jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.Question");
    return ll_cas.ll_getRefValue(addr, casFeatCode_instruction);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setInstruction(int addr, int v) {
        if (featOkTst && casFeat_instruction == null)
      jcas.throwFeatMissing("instruction", "edu.cmu.lti.ntcir.qalab.types.Question");
    ll_cas.ll_setRefValue(addr, casFeatCode_instruction, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Question_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_id = jcas.getRequiredFeatureDE(casType, "id", "uima.cas.String", featOkTst);
    casFeatCode_id  = (null == casFeat_id) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_id).getCode();

 
    casFeat_contextData = jcas.getRequiredFeatureDE(casType, "contextData", "edu.cmu.lti.ntcir.qalab.types.Data", featOkTst);
    casFeatCode_contextData  = (null == casFeat_contextData) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_contextData).getCode();

 
    casFeat_setinstruction = jcas.getRequiredFeatureDE(casType, "setinstruction", "edu.cmu.lti.ntcir.qalab.types.SetInstruction", featOkTst);
    casFeatCode_setinstruction  = (null == casFeat_setinstruction) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_setinstruction).getCode();

 
    casFeat_qdataList = jcas.getRequiredFeatureDE(casType, "qdataList", "uima.cas.FSList", featOkTst);
    casFeatCode_qdataList  = (null == casFeat_qdataList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_qdataList).getCode();

 
    casFeat_questionType = jcas.getRequiredFeatureDE(casType, "questionType", "uima.cas.String", featOkTst);
    casFeatCode_questionType  = (null == casFeat_questionType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_questionType).getCode();

 
    casFeat_knowledgeType = jcas.getRequiredFeatureDE(casType, "knowledgeType", "uima.cas.String", featOkTst);
    casFeatCode_knowledgeType  = (null == casFeat_knowledgeType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_knowledgeType).getCode();

 
    casFeat_instruction = jcas.getRequiredFeatureDE(casType, "instruction", "edu.cmu.lti.ntcir.qalab.types.Instruction", featOkTst);
    casFeatCode_instruction  = (null == casFeat_instruction) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_instruction).getCode();

  }
}



    