package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.SearchIndex;

@Repository
public interface IndexRepository extends JpaRepository<SearchIndex, Integer> {
    @Query(value = "select * from search_index t where t.page_id = :pageId and t.lemma_id = :lemmaId",nativeQuery = true)
    SearchIndex searchIndexExists(@Param("pageId")Integer pageId, @Param("lemmaId")Integer lemmaId);
}
