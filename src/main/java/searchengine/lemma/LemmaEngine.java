package searchengine.lemma;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.config.LemmaConfiguration;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class LemmaEngine implements LemmaService {

    private final LemmaConfiguration lemmaConfiguration;

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

    public List<String> getLemma(String word) throws IOException {
        List<String> lemmaList = new ArrayList<>();
        if (checkLanguage(word).equals("Russian")) {
            List<String> baseRusForm = lemmaConfiguration.russianLuceneMorphology().getNormalForms(word);
            if (!word.isEmpty() && !isCorrectWordForm(word)) {
                lemmaList.addAll(baseRusForm);
            }
        }
        return lemmaList;
    }

    private boolean isCorrectWordForm(String word) throws IOException {
        List<String> morphForm = lemmaConfiguration.russianLuceneMorphology().getMorphInfo(word);
        for (String l : morphForm) {
            if (l.contains("ПРЕДЛ") || l.contains("СОЮЗ") || l.contains("МЕЖД") || l.contains("ВВОДН") || l.contains("ЧАСТ") || l.length() <= 3) {
                return true;
            }
        }
        return false;
    }


    private String checkLanguage(String word) {
        String russianAlphabet = "[а-яА-Я]+";
        String englishAlphabet = "[a-zA-Z]+";

        if (word.matches(russianAlphabet)) {
            return "Russian";
        } else if (word.matches(englishAlphabet)) {
            return "English";
        } else {
            return "";
        }
    }

    private String arrayContainsWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ").trim();
    }

    public Collection<Integer> findLemmaIndexInText(String content, String lemma) throws IOException {
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] elements = content.toLowerCase(Locale.ROOT)
                .split("\\p{Punct}|\\s");
        int index = 0;
        List<String> lemmas;
        for (String el : elements) {
            lemmas = getLemma(el);
            for (String lem : lemmas) {
                if (lem.equals(lemma)) {
                    lemmaIndexList.add(index);
                }
            }
            index += el.length() + 1;
        }
        return lemmaIndexList;
    }
}
