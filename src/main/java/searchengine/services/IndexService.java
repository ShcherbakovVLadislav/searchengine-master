package searchengine.services;

import searchengine.model.SitesPageTable;

public interface IndexService {
    void indexHtml(String html, SitesPageTable indexingPage);
}
