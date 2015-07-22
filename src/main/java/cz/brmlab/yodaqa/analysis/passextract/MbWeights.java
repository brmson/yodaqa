package cz.brmlab.yodaqa.analysis.passextract;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jblas.DoubleMatrix;

/**
 * Created by silvicek on 7/22/15.
 */



public final class MbWeights {
	private static MbWeights mbw = new MbWeights();

	private final DoubleMatrix M;
	private final double b;
	private final String path="src/main/resources/cz/brmlab/yodaqa/analysis/passextract/Mb.txt";

	private MbWeights(){
		DoubleMatrix M=DoubleMatrix.zeros(50,50);
		double b=0;
		File f=new File(path);
		FileReader fr = null;
		try {
			fr = new FileReader(f);
		} catch (FileNotFoundException ex) {
			Logger.getLogger(MbWeights.class.getName()).log(Level.SEVERE, null, ex);
		}
		BufferedReader br=new BufferedReader(fr);
		String line;
		try {
			for(int j=0;j<50;j++){
				line=br.readLine();
				String [] numbers=line.split(" ");
				for(int i=0;i<50;i++){
					M.put(j,i, Double.parseDouble(numbers[i]));
				}
			}
			line=br.readLine();
			b=Double.parseDouble(line);
		} catch (IOException ex) {
			Logger.getLogger(MbWeights.class.getName()).log(Level.SEVERE, null, ex);
		}
		this.M=M;
		this.b=b;
	}

	public DoubleMatrix getM() {
		return M;
	}

	public double getB() {
		return b;
	}

	public static MbWeights getInstance() {
		return mbw;
	}

}
