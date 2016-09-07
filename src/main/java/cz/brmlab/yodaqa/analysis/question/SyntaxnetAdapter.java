package cz.brmlab.yodaqa.analysis.question;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SyntaxnetAdapter extends JCasAnnotator_ImplBase{

    final Logger logger = LoggerFactory.getLogger(CzechPOSTagger.class);
    private HttpURLConnection conn = null;
    private static String URL_STRING;
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        URL_STRING = "http://cloud.ailao.eu:4570/czech_parser";//PrivateResources.getInstance().getResource("lemma_url");
        if (URL_STRING == null) {
            logger.warn("Czech pos tagger not used (URL not specified)");
        }
    }


    private String createRequest(List<Token> tokens) {
        String result = "";
        for(Token tok: tokens) {
            result += tok.getCoveredText() + " ";
        }
        return result;
    }

    private InputStream sendRequest(String data) {
        while(true) {
            try {
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "text/plain");

                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes());
                os.flush();
                return conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                logger.info("Service unavailable. Retrying...");
                try {
                    TimeUnit.SECONDS.sleep(10);
                    conn.disconnect();
                } catch (Exception ee) {
                }
            }
        }
    }

    public ArrayList<String> parseResponse(String response){
        ArrayList<String> result = new ArrayList();
        String[] lines = response.split("\n");
        for (String line : lines) {
            result.add(line.split("\t")[3]);
        }
        return result;
    }

    public String processResponse(InputStream is) throws IOException{
        int i;
        String response = "";
        while((i=is.read())!=-1){
            response += (char)i;
        }
        return response;
    }

    private void addTagToTokens(JCas jCas, List<Token> tokens, ArrayList<String> tags) {
        //FIXME Add list size check
        for (int i = 0; i < tokens.size(); i++) {
            Token tok = tokens.get(i);
            logger.debug("Token: {}/{} {}", tok.getCoveredText(), tags.get(i));

            if (tok.getPos() == null) {
                POS pos = new POS(jCas);
                pos.setBegin(tok.getBegin());
                pos.setEnd(tok.getEnd());
                pos.addToIndexes();
                tok.setPos(pos);
            }
            tok.getPos().setPosValue(tags.get(i));


        }
    }

    public void process(JCas jCas) throws AnalysisEngineProcessException{
        if (URL_STRING == null)
            return;
        //logger.debug("Czech pos tagger");
        List<Token> tokens = new ArrayList(JCasUtil.select(jCas, Token.class));
        URL url;
        try {
            url = new URL(URL_STRING);
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String request = createRequest(tokens);
        InputStream is = sendRequest(request);
        String response = "";
        try {
            response = processResponse(is);
        } catch (IOException e){
            e.printStackTrace();
        }
        addTagToTokens(jCas, tokens, parseResponse(response));
        conn.disconnect();
        if (jCas.getDocumentText() != null) {
            ROOT r = new ROOT(jCas, 0, jCas.getDocumentText().length());
            r.addToIndexes();
        }
    }
}

