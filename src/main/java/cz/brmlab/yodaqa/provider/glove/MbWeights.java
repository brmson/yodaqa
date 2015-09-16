package cz.brmlab.yodaqa.provider.glove;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jblas.DoubleMatrix;

/**
 * Holds weights for sentence(property)-selection features.
 * More at https://github.com/brmson/Sentence-selection.
 */
public class MbWeights {

	private final DoubleMatrix M;
	private final double b;

	public MbWeights(InputStream path) {
		DoubleMatrix M = DoubleMatrix.zeros(50, 50);
		double b = 0;
		BufferedReader br = new BufferedReader(new InputStreamReader(path));
		String line;
		try {
			for (int j = 0; j < 50; j++) {
				line = br.readLine();
				if(line.startsWith("\\\\")) {
					j--;
					continue;
				}
				String[] numbers = line.split(" ");
				for (int i = 0; i < 50; i++) {
					M.put(j, i, Double.parseDouble(numbers[i]));
				}
			}
			line = br.readLine();
			b = Double.parseDouble(line);
		} catch (IOException ex) {
			Logger.getLogger(MbWeights.class.getName()).log(Level.SEVERE, null, ex);
		}
		this.M = M;
		this.b = b;
	}

	public DoubleMatrix getM() {
		return M;
	}

	public double getB() {
		return b;
	}
}
