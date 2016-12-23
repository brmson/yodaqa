package cz.brmlab.yodaqa.provider.url;

import java.util.HashMap;

/**
 * Created by bzhao on 12/21/16.
 */
public class BackendUrlGroup extends HashMap<String,String> {
    public String getUrl(String urlkey) {
        return this.get(urlkey);
    }
}
