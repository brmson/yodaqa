package cz.brmlab.yodaqa.provider.crf;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.jar.JarInputStream;

import org.cleartk.ml.Feature;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.jar.JarStreams;

import com.github.jcrfsuite.CrfTagger;
import com.github.jcrfsuite.util.Pair;

import third_party.org.chokkan.crfsuite.Attribute;
import third_party.org.chokkan.crfsuite.Item;
import third_party.org.chokkan.crfsuite.ItemSequence;

/** A frontend to the jcrfsuite wrapper.  This is a singleton,
 * obtain an instance via CRFSuite.getInstance(). */
public class CRFSuite {
	/* CRFSuite is a per-thread singleton. */
	/* XXX: We assume there is just a single model file at this point. */
	private static final ThreadLocal<CRFSuite> crfs =
		new ThreadLocal<CRFSuite>() {
			@Override protected CRFSuite initialValue() {
				return new CRFSuite();
			}
		};
	public static CRFSuite getInstance() {
		return crfs.get();
	}

	protected File modelFile;
	protected CrfTagger tagger;

	protected CRFSuite() {
		tagger = new CrfTagger(modelFileFromJar());
	}

	/** Create a tagging of a given list of items.  Each item is
	 * represented as a list of features. */
	public CRFTagging tag(List<List<Feature>> featureLists) {
		ItemSequence xseq = featuresToItemSequence(featureLists);
		List<Pair<String, Double>> pairList = tagger.tag(xseq);
		return new CRFTagging(pairList);
	}

	ItemSequence featuresToItemSequence(List<List<Feature>> featureLists) {
		ItemSequence xseq = new ItemSequence();
		for (List<Feature> featureList : featureLists) {
			Item item = new Item();
			for (Feature f : featureList)
				item.add(new Attribute(f.getName() + "_" + f.getValue()));
			xseq.add(item);
		}
		return xseq;
	}

	/** Do some crazy things to extract the model file from model.jar
	 * and return a temporary file name that can be used to refer to it. */
	protected String modelFileFromJar() {
		/* XXX: Hardcoded stuff. */
		// Also hardcoded in analysis/passage/biotagger/CanByBIOTaggerAE.java
		String modelDir = "data/ml/biocrf";

		// XXX this is really whacky stuff! but straightforward by-the-recipe
		File modelJarFile = JarClassifierBuilder.getModelJarFile(modelDir);
		InputStream stream = null;
		try {
			stream = new FileInputStream(modelJarFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		stream = new BufferedInputStream(stream);
		try {
			JarInputStream modelStream = new JarInputStream(stream);
			JarStreams.getNextJarEntry(modelStream, "encoders.ser");
			JarStreams.getNextJarEntry(modelStream, "crfsuite.model");
			this.modelFile = File.createTempFile("model", ".crfsuite");
			this.modelFile.deleteOnExit();

			InputStream inputStream = new DataInputStream(modelStream);
			OutputStream out = new FileOutputStream(modelFile);
			byte buf[] = new byte[1024];
			int len;
			while ((len = inputStream.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try { stream.close(); } catch (IOException e) { e.printStackTrace(); }
		}

		return this.modelFile.getAbsolutePath();
	}
}
