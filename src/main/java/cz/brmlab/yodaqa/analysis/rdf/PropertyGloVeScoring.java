package cz.brmlab.yodaqa.analysis.rdf;

import cz.brmlab.yodaqa.provider.glove.MbWeights;
import cz.brmlab.yodaqa.provider.glove.Relatedness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by silvicek on 8/18/15.
 */
public class PropertyGloVeScoring {

	private static PropertyGloVeScoring pgs=new PropertyGloVeScoring();

	private Relatedness r=new Relatedness(new MbWeights(PropertyGloVeScoring.class.getResourceAsStream("Mbprop.txt")));

	public double relatedness(String q,String prop){
		List<String> ql=new ArrayList<>(Arrays.asList(q.toLowerCase().split("\\W+")));
		List<String> a=new ArrayList<>(Arrays.asList(prop.toLowerCase().split("\\W+")));

		double res=r.probability(ql,a);
		return res;
	}


	public static PropertyGloVeScoring getInstance(){
		return pgs;
	}
}
