package searchengine.services.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.SearchIndex;
import searchengine.model.SitesPageTable;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.services.IndexService;
import searchengine.services.LemmaService;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.HashMap;

@Slf4j
@Service
@AllArgsConstructor
public class IndexServiceImpl implements IndexService {

    private LemmaService lemmaService;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    @Override
    public void indexHtml(String html, SitesPageTable indexingPage) {
        long start = System.currentTimeMillis();
        try {
            HashMap<String, Integer> lemmas = lemmaService.getLemmasFromText(html);
            lemmas.entrySet().parallelStream()
                    .forEach(entry -> saveLemma(entry.getKey(), entry.getValue(), indexingPage));
            log.warn("Индексация страницы " + (System.currentTimeMillis() - start) + " lemmas:" + lemmas.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Transactional
    private void saveLemma(String key, Integer value, SitesPageTable indexingPage) {
        Lemma existLemmaInDB = lemmaRepository.lemmaExists(key);
        if (existLemmaInDB != null) {
            existLemmaInDB.setFrequency(existLemmaInDB.getFrequency() + value);
            lemmaRepository.saveAndFlush(existLemmaInDB);
            createIndex(indexingPage, existLemmaInDB, value);
        } else {
            try {
                Lemma lemma = new Lemma();
                lemma.setLemma(key);
                lemma.setFrequency(value);
                lemma.setSiteId(indexingPage.getSiteId());
                lemma.setSiteTable(indexingPage.getSiteTable());
                lemmaRepository.saveAndFlush(lemma);
                createIndex(indexingPage, lemma, value);
            } catch (DataIntegrityViolationException e) {
                saveLemma(key, value, indexingPage);
            }
        }
    }

    private void createIndex(SitesPageTable indexingPage, Lemma lemmaInDB, Integer count) {
        SearchIndex searchIndexExists = indexRepository.searchIndexExists(indexingPage.getId(), lemmaInDB.getId());
        if (searchIndexExists != null) {
            searchIndexExists.setLemmaCount(searchIndexExists.getLemmaCount() + count);
            indexRepository.save(searchIndexExists);
        } else {
            SearchIndex index = new SearchIndex();
            index.setLemmaId(lemmaInDB.getId());
            index.setLemmaCount(count);
            index.setLemma(lemmaInDB);
            index.setPageId(indexingPage.getId());
            index.setSitesPageTable(indexingPage);
            indexRepository.save(index);
        }
    }
}
