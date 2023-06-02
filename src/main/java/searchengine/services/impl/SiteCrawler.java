package searchengine.services.impl;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.Connection;
import searchengine.model.SitesPageTable;
import searchengine.model.SiteTable;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class SiteCrawler extends RecursiveAction {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final AtomicBoolean indexingProcessing;
    private final Connection connection;
    private final Set<String> urlSet = new HashSet<>();
    private final String page;
    private final SiteTable siteDomain;
    private final ConcurrentHashMap<String, SitesPageTable> resultForkJoinPoolIndexedPages;

    public SiteCrawler(SiteRepository siteRepository, PageRepository pageRepository, SiteTable siteDomain, String page, ConcurrentHashMap<String, SitesPageTable> resultForkJoinPoolIndexedPages, Connection connection, AtomicBoolean indexingProcessing) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.page = page;
        this.resultForkJoinPoolIndexedPages = resultForkJoinPoolIndexedPages;
        this.connection = connection;
        this.indexingProcessing = indexingProcessing;
        this.siteDomain = siteDomain;
    }

    @SneakyThrows
    @Override
    protected void compute() {
        if (resultForkJoinPoolIndexedPages.get(page) != null || !indexingProcessing.get()) {
            return;
        }
        SitesPageTable indexingSitesPageTable = new SitesPageTable();
        indexingSitesPageTable.setPath(page);
        indexingSitesPageTable.setSiteId(siteDomain.getId());
        try {
            org.jsoup.Connection connect = Jsoup.connect(siteDomain.getUrl() + page).userAgent(connection.getUserAgent()).referrer(connection.getReferer());
            Document doc = connect.timeout(60000).get();
            indexingSitesPageTable.setContent(doc.head() + String.valueOf(doc.body()));
            Elements pages = doc.getElementsByTag("a");
            for (org.jsoup.nodes.Element element : pages)
                if (!element.attr("href").isEmpty() && element.attr("href").charAt(0) == '/') {
                    if (resultForkJoinPoolIndexedPages.get(page) != null || !indexingProcessing.get()) {
                        return;
                    } else if (resultForkJoinPoolIndexedPages.get(element.attr("href")) == null) {
                        urlSet.add(element.attr("href"));
                    }
                }
            indexingSitesPageTable.setCode(doc.connection().response().statusCode());
        } catch (Exception e) {
            String message = e.toString();
            int errorCode;
            if (message.contains("UnsupportedMimeTypeException")) {
                errorCode = 415;
            } else if (message.contains("Status=401")) {
                errorCode = 401;
            } else if (message.contains("UnknownHostException")) {
                errorCode = 401;
            } else if (message.contains("Status=403")) {
                errorCode = 403;
            } else if (message.contains("Status=404")) {
                errorCode = 404;
            } else if (message.contains("Status=500")) {
                errorCode = 401;
            } else if (message.contains("ConnectException: Connection refused")) {
                errorCode = 500;
            } else if (message.contains("SSLHandshakeException")) {
                errorCode = 525;
            } else if (message.contains("Status=503")) {
                errorCode = 503;
            } else {
                errorCode = -1;
            }
            indexingSitesPageTable.setCode(errorCode);
        }
        if (resultForkJoinPoolIndexedPages.get(page) != null || !indexingProcessing.get()) {
            return;
        }
        resultForkJoinPoolIndexedPages.putIfAbsent(indexingSitesPageTable.getPath(), indexingSitesPageTable);
        SiteTable siteTable = siteRepository.findById(siteDomain.getId()).orElseThrow();
        siteTable.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
        siteRepository.save(siteTable);
        pageRepository.save(indexingSitesPageTable);
        List<SiteCrawler> indexingPagesTasks = new ArrayList<>();
        for (String url : urlSet) {
            if (resultForkJoinPoolIndexedPages.get(url) == null && indexingProcessing.get()) {
                SiteCrawler task = new SiteCrawler(siteRepository, pageRepository, siteTable, url, resultForkJoinPoolIndexedPages, connection, indexingProcessing);
                task.fork();
                indexingPagesTasks.add(task);
            }
        }
        for (SiteCrawler page : indexingPagesTasks) {
            if (!indexingProcessing.get()) {
                return;
            }
            page.join();
        }

    }

}
