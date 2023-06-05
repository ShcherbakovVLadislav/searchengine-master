package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.SearchIndex;
import searchengine.model.SitesPageTable;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<SearchIndex, Integer> {
    @Query(value = "select * from search_index t where t.page_id = :pageId and t.lemma_id = :lemmaId",nativeQuery = true)
    SearchIndex searchIndexExists(@Param("pageId")Integer pageId, @Param("lemmaId")Integer lemmaId);

    @Transactional
    @Query(value = "select * from Search_index where Search_index.lemma_id in :lemmas and Search_index.page_id in :pages", nativeQuery = true)
    List<SearchIndex> findByPageAndLemmas(@Param("lemmas") List<Lemma> lemmaList,
                                          @Param("pages") List<SitesPageTable> pages);
}
