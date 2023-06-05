package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.SiteTable;
import searchengine.model.SitesPageTable;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<SitesPageTable, Integer> {
    @Transactional
    long countBySiteId(SiteTable siteId);

    @Transactional
    SitesPageTable getById(long pageID);

    @Transactional
    Iterable<SitesPageTable> findBySiteId(SiteTable sitePath);

    @Transactional
    @Query(value = "SELECT * FROM Search_index JOIN Page  ON Page.id = Search_index.page_id WHERE Search_index.lemma_id IN :lemmas", nativeQuery = true)
    List<SitesPageTable> findByLemmaList(@Param("lemmas") Collection<Lemma> lemmas);
}
