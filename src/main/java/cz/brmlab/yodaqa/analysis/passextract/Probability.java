package cz.brmlab.yodaqa.analysis.passextract;

import cz.brmlab.yodaqa.model.SearchResult.Passage;
import org.jblas.DoubleMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by silvicek on 7/22/15.
 */
public class Probability {
	private static Probability p = new Probability();
	private WWeights w;
	private Relatedness r;
	private Map<String,Double> idf;
	private int N;

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
		for(String s:qtext){
			double f= Collections.frequency(atext, s);
			count+=f/N;

			if(f>0&&idf.get(s)==null){
				System.out.println("SOMETHINGs WRONG");
				System.out.println("Q="+qtext);
				System.out.println("A="+atext);
				System.exit(1);
			}
			if(f>0&&idf.get(s)!=null){
				idfcount+=f/N * Math.log(N / idf.get(s));
			}
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
	public void setidf(Collection<Passage> psg){
//		System.out.println("words size: "+words.size());
		Map<String,Double> idf=new HashMap<>();
		for(Passage passage:psg){
			Set<String> sentence=new HashSet<>(Arrays.asList(passage.getCoveredText().toLowerCase().split("\\W+")));
			for(String word:sentence){
				if(idf.containsKey(word)){
					idf.put(word, idf.get(word)+1);
				}else{
					idf.put(word, 1.0);
				}
			}
		}
		this.idf=idf;
		this.N=psg.size();
	}

	public static Probability getInstance(){
		return p;
	}
}
