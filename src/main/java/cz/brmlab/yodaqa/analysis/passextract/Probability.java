package cz.brmlab.yodaqa.analysis.passextract;

import org.jblas.DoubleMatrix;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by silvicek on 7/22/15.
 */
public class Probability {
	private static Probability p = new Probability();
	private WWeights w;
	private Relatedness r;
	private Map<String,Double> idf;

	private Probability(){
		this.w=WWeights.getInstance();
		this.r=Relatedness.getInstance();
	}

	/** Counts probability using word counts. */
	public double[] probability(List<String> qtext,List<String> atext){
		double p1=r.probability(qtext, atext);
//		System.out.println("p1="+p1);
		double count=0;
		double idfcount=0;
		for(String s:atext){
			double f= Collections.frequency(qtext, s);
			count+=f;
			if(idf.get(s)!=null&& f>0)idfcount+=Math.log(idf.get(s)/f);
		}
//		System.out.println("Qtext= "+qtext.toString());
//		System.out.println("Atext= "+atext.toString());
//		System.out.println("Count="+count);
//		System.out.println("IDFCount="+idfcount);
		DoubleMatrix x=new DoubleMatrix(new double[][] {{p1,count,idfcount,1}});
		double p2= 1/(1+Math.exp(-x.mmul(w.getW()).get(0)));
//		double[] res={p2,count,idfcount};
		double[] res={p1,count,idfcount};
		return res;
	}


	/** Returns map of word counts. */
	public void setidf(List<String> words){
//		System.out.println("words size: "+words.size());
		Map<String,Double> idf=new HashMap<>();
		for(String word:words){
			if(idf.containsKey(word)){
				idf.put(word, idf.get(word)+1);
			}else{
				idf.put(word, 1.0);
			}
		}
		this.idf=idf;
//		System.out.println("IDF size="+idf.size());
//		System.out.println("harry times "+idf.get("harry"));
//		System.out.println("potter times "+idf.get("potter"));
	}

	public static Probability getInstance(){
		return p;
	}
}
