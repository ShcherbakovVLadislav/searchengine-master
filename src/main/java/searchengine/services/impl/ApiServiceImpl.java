package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SitesPageTable;
import searchengine.model.SiteTable;
import searchengine.model.SiteStatusType;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.ApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ApiServiceImpl implements ApiService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList indexingSites;
    private final Set<SiteTable> sitesPageTablesFromDB;
    private final Connection connection;
    private AtomicBoolean indexingProcessing;



    @Override
    public void startIndexing(AtomicBoolean indexingProcessing) {
        this.indexingProcessing = indexingProcessing;
        try {
            deleteDataInDB();
            addSitesToDB();
            indexAllSites();
        } catch (RuntimeException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void deleteDataInDB() {
        List<SiteTable> sitesFromDB = siteRepository.findAll();
        for (SiteTable siteTableDb : sitesFromDB) {
            for (Site siteApp : indexingSites.getSites()) {
                if (siteTableDb.getUrl().equals(siteApp.getUrl())) {
                    siteRepository.deleteById(siteTableDb.getId());
                }
            }
        }
    }

    private void addSitesToDB() {
        for (Site siteApp : indexingSites.getSites()) {
            SiteTable siteTableDAO = new SiteTable();
            siteTableDAO.setSiteStatusType(SiteStatusType.INDEXING);
            siteTableDAO.setName(siteApp.getName());
            siteTableDAO.setUrl(String.valueOf(siteApp.getUrl()));
            siteRepository.save(siteTableDAO);
        }

    }

    private void indexAllSites() throws InterruptedException {
        sitesPageTablesFromDB.addAll(siteRepository.findAll());
        List<String> urlToIndexing = new ArrayList<>();
        for (Site siteApp : indexingSites.getSites()) {
            urlToIndexing.add(String.valueOf(siteApp.getUrl()));
        }
        sitesPageTablesFromDB.removeIf(siteTable -> !urlToIndexing.contains(siteTable.getUrl()));
        List<Thread> indexingThreadList = new ArrayList<>();
        for (SiteTable siteDomain : sitesPageTablesFromDB) {
            Runnable indexSite = () -> {
                ConcurrentHashMap<String, SitesPageTable> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
                try {
                    System.out.println("Запущена индексация "+siteDomain.getUrl());
                    new ForkJoinPool().invoke(new SiteCrawler(siteRepository,pageRepository,siteDomain, "", resultForkJoinPageIndexer, connection, indexingProcessing));
                } catch (SecurityException ex) {
                    SiteTable siteTable = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    siteTable.setSiteStatusType(SiteStatusType.FAILED);
                    siteTable.setLastError(ex.getMessage());
                    siteRepository.save(siteTable);
                }
                if (!indexingProcessing.get()) {
                    SiteTable siteTable = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    siteTable.setSiteStatusType(SiteStatusType.FAILED);
                    siteTable.setLastError("Indexing stopped by user");
                    siteRepository.save(siteTable);
                } else {
                    System.out.println("Проиндексирован сайт: " + siteDomain.getName());
                    SiteTable siteTable = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    siteTable.setSiteStatusType(SiteStatusType.INDEXED);
                    siteRepository.save(siteTable);
                }

            };
            Thread thread = new Thread(indexSite);
            indexingThreadList.add(thread);
            thread.start();
        }
        for (Thread thread :indexingThreadList) {
            thread.join();
        }
        indexingProcessing.set(false);
    }
}
