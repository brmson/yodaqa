package cz.brmlab.yodaqa.provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by silvicek on 7/22/15.
 */



/** Contains Glove word embeddings dictionary. */
public final class GloveDictionary {
	private static GloveDictionary gd = new GloveDictionary();
	private final Map<String,double[]> dictionary;
	private final String path="data/glove/glove.6B.50d.txt";

	private GloveDictionary(){
		Map<String,double[]> dictionary=new HashMap<>();
		FileReader fr=null;
		try {
			File f=new File(path);
			fr = new FileReader(f);
			BufferedReader br=new BufferedReader(fr);

			String line;
			while((line=br.readLine())!=null){
				String[] s=line.split(" ");
				String word=s[0];
				double[] gword=new double[s.length-1];
				for(int i=1;i<s.length;i++){
					gword[i-1]=Double.parseDouble(s[i]);
				}
				dictionary.put(word, gword);
			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(GloveDictionary.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(GloveDictionary.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				fr.close();
			} catch (IOException ex) {
				Logger.getLogger(GloveDictionary.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		System.out.println("GloVe dictionary created");
		this.dictionary=dictionary;
	}

	public double[] get(String key){
			return dictionary.get(key);
		}
	public static GloveDictionary getInstance() {
		return gd;
	}
}
