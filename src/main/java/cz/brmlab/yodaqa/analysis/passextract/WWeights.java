package cz.brmlab.yodaqa.analysis.passextract;

import org.jblas.DoubleMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by silvicek on 7/22/15.
 */
public class WWeights {
	private static WWeights ww=new WWeights();
	private final DoubleMatrix w;
	private final String path="weights.txt";

	public WWeights(){
		DoubleMatrix w=DoubleMatrix.zeros(4,1);

		BufferedReader br = new BufferedReader(new InputStreamReader(MbWeights.class.getResourceAsStream(path)));
		String line;
		try {
			int i=0;
			while((line=br.readLine())!=null){
				w.put(i, Double.parseDouble(line));
				i++;
			}
		} catch (IOException ex) {
			Logger.getLogger(WWeights.class.getName()).log(Level.SEVERE, null, ex);
		}
		this.w=w;
	}
	public static WWeights getInstance() {
		return ww;
	}
	public DoubleMatrix getW(){
		return this.w;
	}
}
