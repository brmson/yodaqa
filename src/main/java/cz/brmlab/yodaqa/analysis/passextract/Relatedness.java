package cz.brmlab.yodaqa.analysis.passextract;

import org.jblas.DoubleMatrix;
import eu.ailao.glove.GloveDictionary;
import java.util.List;

/**
 * Created by silvicek on 7/22/15.
 */
public class Relatedness{

	private static Relatedness r=new Relatedness();
	private MbWeights mb;
	private GloveDictionary dict;

	private Relatedness(){
		this.mb=MbWeights.getInstance();
		this.dict=GloveDictionary.getInstance();
	}

	public double probability(List<String> q,List<String> a){
		return probability(gloveBOW(q,this.dict),gloveBOW(a,this.dict));
	}

	/** Counts probability from q,a glove vectors. */
	private double probability(DoubleMatrix q,DoubleMatrix a){
		DoubleMatrix M=this.mb.getM();
		double b=this.mb.getB();
		return 1/(1+Math.exp(-z(q,M,a,b)));
	}

	/** Returns glove vectors from input sentence, dictionary. Uses box-of-words approach. */
	private DoubleMatrix gloveBOW(List<String> words,GloveDictionary dict){
		DoubleMatrix bow = DoubleMatrix.zeros(50,1);
		int w=0;
		for(int i=0;i<words.size();i++){
			double[] x=dict.get(words.get(i));
			if(x!=null){
				DoubleMatrix glove=new DoubleMatrix(x);
				w++;
				bow=bow.add(glove);
			}
		}
		if(w!=0)bow=bow.div(w);
//        System.out.println(words+" = "+bow);
		return bow;
	}
	/** qTMa+b. */
	private double z(DoubleMatrix q,DoubleMatrix M,DoubleMatrix a,double b){
		return q.transpose().mmul(M).mmul(a).get(0)+b;
	}

	public static Relatedness getInstance(){
		return r;
	}
}