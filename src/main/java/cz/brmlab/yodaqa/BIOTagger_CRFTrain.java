package cz.brmlab.yodaqa;

import org.cleartk.ml.jar.Train;


/* BIOTagger_CRFTrain is a gadget to train the passage analysis biotagger
 * model based on previously collected data.  To collect the data, run
 *
 * 	./gradlew tsvgs -Dcz.brmlab.yodaqa.train_ansbiocrf=1
 */

public class BIOTagger_CRFTrain {
        public static void main(String[] args) throws Exception {
		Train.main(args[0]);
        }
}
