package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class LemmaServiceImpl implements LemmaService {

    @Override
    public HashMap<String, Integer> getLemmasFromText(String html) throws IOException {
        HashMap<String, Integer> lemmasInText = new HashMap<>();
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        String text = Jsoup.parse(html).text();
        List<String> words = new ArrayList<>(List.of(text.replaceAll("(?U)\\pP","")
                .toLowerCase().split(" ")));
        for (String word : words) {
            findingLemma(word, luceneMorphology, lemmasInText);
        }
        return lemmasInText;
    }

    private void findingLemma(String word, LuceneMorphology luceneMorphology, HashMap<String, Integer> lemmas) {
        try {
            if (word.isEmpty() || String.valueOf(word.charAt(0)).matches("[0-9]") || String.valueOf(word.charAt(0)).matches("[A-Za-z]")) {
                return;
            }
            List<String> initialForms = luceneMorphology.getNormalForms(word);
            String wordInfo = luceneMorphology.getMorphInfo(word).toString();
            if (!wordInfo.contains("СОЮЗ") && !wordInfo.contains("ПРЕДЛ") && !wordInfo.contains("МЕЖД")) {
                for (String initialWord : initialForms) {
                    if (!lemmas.containsKey(initialWord)) {
                        lemmas.put(initialWord, 1);
                    } else {
                        lemmas.replace(initialWord, lemmas.get(initialWord) + 1);
                    }
                }
            }
        } catch (RuntimeException e) {
            log.debug(e.getMessage());
        }
    }

    @Override
    public void getLemmasFromUrl(URL url) throws IOException {
        org.jsoup.Connection connection = Jsoup.connect(String.valueOf(url));
        Document document = connection.timeout(60000).get();
        HashMap<String,Integer> result = getLemmasFromText(document.body().html());
        System.out.println(result.keySet());
        System.out.println(result.values());
    }
}
