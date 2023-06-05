package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteStatusType;
import searchengine.model.SiteTable;

import javax.transaction.Transactional;

@Repository
public interface SiteRepository extends JpaRepository<SiteTable, Integer> {
    @Transactional
    SiteTable findByUrl(String url);

    @Transactional
    @Query(value = "SELECT site_status_type FROM search_engine.site where site.id = :id + 1", nativeQuery = true)
    String findSiteStatusType(@Param("id") Integer id);

    @Transactional
    @Query(value = "SELECT last_error FROM search_engine.site where id = :id + 1", nativeQuery = true)
    String findLastError(@Param("id") Integer id);
}
