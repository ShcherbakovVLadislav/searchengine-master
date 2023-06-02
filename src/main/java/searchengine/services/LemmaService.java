package searchengine.services;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

public interface LemmaService {
    HashMap<String, Integer> getLemmasFromText(String text) throws IOException;
    void getLemmasFromUrl(URL url) throws IOException;
}
