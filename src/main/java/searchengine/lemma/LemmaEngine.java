package searchengine.lemma;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.config.LemmaConfiguration;
import searchengine.exception.CurrentRuntimeException;

import java.io.IOException;
import java.util.*;

@Component
@Slf4j
public record LemmaEngine(LemmaConfiguration lemmaConfiguration) {
    public Map<String, Integer> getLemmaMap(String text) {
        text = arrayContainsWords(text);
        Map<String, Integer> lemmaList = new HashMap<>();
        String[] elements = text.toLowerCase(Locale.ROOT).split("\\s+");
        List<String> wordsList;
        int count;
        for (String el : elements) {
            try {
                wordsList = getLemma(el);
            } catch (Exception e) {
                throw new CurrentRuntimeException(e.getMessage());
            }
            for (String word : wordsList) {
                count = lemmaList.getOrDefault(word, 0);
                lemmaList.put(word, count + 1);
            }
        }
        return lemmaList;
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
