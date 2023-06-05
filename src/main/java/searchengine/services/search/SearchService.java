package searchengine.services.search;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.dto.SearchDto;
import searchengine.exception.CurrentIOException;
import searchengine.lemma.LemmaEngine;
import searchengine.model.SearchIndex;
import searchengine.model.Lemma;
import searchengine.model.SitesPageTable;
import searchengine.model.SiteTable;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public record SearchService(LemmaEngine lemmaEngine, LemmaRepository lemmaRepository, PageRepository pageRepository, IndexRepository indexRepository) {

    private List<SearchDto> getSearchDtoList(ConcurrentHashMap<SitesPageTable, Float> pageList,
                                             List<String> textLemmaList) {
        List<SearchDto> searchDtoList = new ArrayList<>();
        StringBuilder titleStringBuilder = new StringBuilder();
        for (SitesPageTable page : pageList.keySet()) {
            String url = page.getPath();
            String content = page.getContent();
            SiteTable pageSite = page.getSiteTable();
            String site = pageSite.getUrl();
            String siteName = pageSite.getName();
            String title = clearCodeFromTag(content, "title");
            String body = clearCodeFromTag(content, "body");
            titleStringBuilder.append(title).append(body);
            float pageValue = pageList.get(page);
            List<Integer> lemmaIndex = new ArrayList<>();
            for (String lemma : textLemmaList) {
                try {
                    lemmaIndex.addAll(lemmaEngine.findLemmaIndexInText(titleStringBuilder.toString(), lemma));
                } catch (IOException e) {
                    new CurrentIOException(e.getMessage());
                }
            }
            Collections.sort(lemmaIndex);
            StringBuilder snippetBuilder = new StringBuilder();
            List<String> wordList = getWordsFromSiteContent(titleStringBuilder.toString(), lemmaIndex);
            for (int y = 0; y < wordList.size(); y++) {
                snippetBuilder.append(wordList.get(y)).append(".");
                if (y > 3) {
                    break;
                }
            }
            searchDtoList.add(new SearchDto(site, siteName, url, title, snippetBuilder.toString(), pageValue));
        }
        return searchDtoList;
    }

    private List<String> getWordsFromSiteContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int next = i + 1;
            while (next < lemmaIndex.size() && 0 < lemmaIndex.get(next) - end && lemmaIndex.get(next) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(next));
                next += 1;
            }
            i = next - 1;
            String word = content.substring(start, end);
            int startIndex;
            int nextIndex;
            if (content.lastIndexOf(" ", start) != -1) {
                startIndex = content.lastIndexOf(" ", start);
            } else startIndex = start;
            if (content.indexOf(" ", end + lemmaIndex.size() / 10) != -1) {
                nextIndex = content.indexOf(" ", end + lemmaIndex.size() / 10);
            } else nextIndex = content.indexOf(" ", end);
            String text = content.substring(startIndex, nextIndex).replaceAll(word, "<b>".concat(word).concat("</b>"));
            result.add(text);
        }
        result.sort(Comparator.comparing(String::length).reversed());
        return result;
    }

    private Map<SitesPageTable, Float> getRelevanceFromPage(List<SitesPageTable> pageList,
                                                       List<SearchIndex> indexList) {
        Map<SitesPageTable, Float> relevanceMap = new HashMap<>();
        for (SitesPageTable page : pageList) {
            float relevance = 0;
            for (SearchIndex index : indexList) {
                if (index.getSitesPageTable() == page) {
                    relevance += index.getRank();
                }
            }
            relevanceMap.put(page, relevance);
        }

        Map<SitesPageTable, Float> allRelevanceMap = new HashMap<>();

        relevanceMap.keySet().forEach(page -> {
            float relevance = relevanceMap.get(page) / Collections.max(relevanceMap.values());
            allRelevanceMap.put(page, relevance);
        });

        List<Map.Entry<SitesPageTable, Float>> sortList = new ArrayList<>(allRelevanceMap.entrySet());
        sortList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        Map<SitesPageTable, Float> map = new ConcurrentHashMap<>();
        Entry<SitesPageTable, Float> pageModelFloatEntry;
        for (int y = 0; y < sortList.size(); y++) {
            pageModelFloatEntry = sortList.get(y);
            map.putIfAbsent(pageModelFloatEntry.getKey(), pageModelFloatEntry.getValue());
        }
        return map;
    }

    public List<Lemma> getLemmaFromSite(List<String> lemmas, SiteTable site) {
        lemmaRepository.flush();
        List<Lemma> lemmaModels = lemmaRepository.findLemmaListBySite(lemmas, site);
        List<Lemma> result = new ArrayList<>(lemmaModels);
        result.sort(Comparator.comparingInt(Lemma::getFrequency));
        return result;
    }

    public List<String> getLemmaFromSearchText(String text) {
        String[] words = text.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        List<String> list;
        for (String lemma : words) {
            try {
                list = lemmaEngine.getLemma(lemma);
                lemmaList.addAll(list);
            } catch (IOException e) {
                new CurrentIOException(e.getMessage());
            }
        }
        return lemmaList;
    }

    public List<SearchDto> createSearchDtoList(List<Lemma> lemmaList,
                                               List<String> textLemmaList,
                                               int start, int limit) {
        List<SearchDto> result = new ArrayList<>();
        pageRepository.flush();
        if (lemmaList.size() >= textLemmaList.size()) {
            List<SitesPageTable> pagesList = pageRepository.findByLemmaList(lemmaList);
            indexRepository.flush();
            List<SearchIndex> indexesList = indexRepository.findByPageAndLemmas(lemmaList, pagesList);
            Map<SitesPageTable, Float> relevanceMap = getRelevanceFromPage(pagesList, indexesList);
            List<SearchDto> searchDtos = getSearchDtoList((ConcurrentHashMap<SitesPageTable, Float>) relevanceMap, textLemmaList);
            if (start > searchDtos.size()) {
                return new ArrayList<>();
            }
            if (searchDtos.size() > limit) {
                for (int i = start; i < limit; i++) {
                    result.add(searchDtos.get(i));
                }
                return result;
            } else return searchDtos;

        } else return result;
    }

    public  String clearCodeFromTag(String text, String element) {
        Document doc = Jsoup.parse(text);
        Elements elements = doc.select(element);
        String html = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(html).text();
    }

}
