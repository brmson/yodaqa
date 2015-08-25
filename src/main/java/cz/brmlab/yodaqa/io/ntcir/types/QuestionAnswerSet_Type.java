
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
public class QuestionAnswerSet_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (QuestionAnswerSet_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = QuestionAnswerSet_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new QuestionAnswerSet(addr, QuestionAnswerSet_Type.this);
  			   QuestionAnswerSet_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new QuestionAnswerSet(addr, QuestionAnswerSet_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = QuestionAnswerSet.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
 
  /** @generated */
  final Feature casFeat_question;
  /** @generated */
  final int     casFeatCode_question;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getQuestion(int addr) {
        if (featOkTst && casFeat_question == null)
      jcas.throwFeatMissing("question", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    return ll_cas.ll_getRefValue(addr, casFeatCode_question);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setQuestion(int addr, int v) {
        if (featOkTst && casFeat_question == null)
      jcas.throwFeatMissing("question", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    ll_cas.ll_setRefValue(addr, casFeatCode_question, v);}
    
  
 
  /** @generated */
  final Feature casFeat_answerChoiceList;
  /** @generated */
  final int     casFeatCode_answerChoiceList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAnswerChoiceList(int addr) {
        if (featOkTst && casFeat_answerChoiceList == null)
      jcas.throwFeatMissing("answerChoiceList", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    return ll_cas.ll_getRefValue(addr, casFeatCode_answerChoiceList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAnswerChoiceList(int addr, int v) {
        if (featOkTst && casFeat_answerChoiceList == null)
      jcas.throwFeatMissing("answerChoiceList", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    ll_cas.ll_setRefValue(addr, casFeatCode_answerChoiceList, v);}
    
  
 
  /** @generated */
  final Feature casFeat_analyzedAnswerChoiceList;
  /** @generated */
  final int     casFeatCode_analyzedAnswerChoiceList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAnalyzedAnswerChoiceList(int addr) {
        if (featOkTst && casFeat_analyzedAnswerChoiceList == null)
      jcas.throwFeatMissing("analyzedAnswerChoiceList", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    return ll_cas.ll_getRefValue(addr, casFeatCode_analyzedAnswerChoiceList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAnalyzedAnswerChoiceList(int addr, int v) {
        if (featOkTst && casFeat_analyzedAnswerChoiceList == null)
      jcas.throwFeatMissing("analyzedAnswerChoiceList", "edu.cmu.lti.ntcir.qalab.types.QuestionAnswerSet");
    ll_cas.ll_setRefValue(addr, casFeatCode_analyzedAnswerChoiceList, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public QuestionAnswerSet_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_question = jcas.getRequiredFeatureDE(casType, "question", "edu.cmu.lti.ntcir.qalab.types.Question", featOkTst);
    casFeatCode_question  = (null == casFeat_question) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_question).getCode();

 
    casFeat_answerChoiceList = jcas.getRequiredFeatureDE(casType, "answerChoiceList", "uima.cas.FSList", featOkTst);
    casFeatCode_answerChoiceList  = (null == casFeat_answerChoiceList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_answerChoiceList).getCode();

 
    casFeat_analyzedAnswerChoiceList = jcas.getRequiredFeatureDE(casType, "analyzedAnswerChoiceList", "uima.cas.FSList", featOkTst);
    casFeatCode_analyzedAnswerChoiceList  = (null == casFeat_analyzedAnswerChoiceList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_analyzedAnswerChoiceList).getCode();

  }
}



    