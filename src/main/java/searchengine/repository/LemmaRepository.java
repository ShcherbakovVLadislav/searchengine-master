package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.SiteTable;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Query(value = "select * from lemma t where t.lemma = :lemma for update", nativeQuery = true)
    Lemma lemmaExists(@Param("lemma") String lemma);

    @Modifying
    @Query(value = "update Lemma t set t.frequency = t.frequency + :frequency where t.id = :idLemma")
    void incrementFrequency(Integer idLemma, Integer frequency);

    @Transactional
    Lemma getById(long lemmaID);

    @Transactional
    long countBySiteId(SiteTable site);

    @Transactional
    List<Lemma> findBySiteId(SiteTable siteId);
    @Transactional
    @Query(value = "select * from Lemma where Lemma.lemma in :lemmas AND Lemma.site_id = :site", nativeQuery = true)
    List<Lemma> findLemmaListBySite(List<String> lemmas, SiteTable site);
}
