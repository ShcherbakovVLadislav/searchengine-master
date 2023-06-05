package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteStatusType;
import searchengine.model.SiteTable;

import javax.transaction.Transactional;

@Repository
public interface SiteRepository extends JpaRepository<SiteTable, Integer> {
    @Transactional
    SiteTable findByUrl(String url);

    @Query(value = "SELECT site_status_type FROM search_engine.site where site.id = :i", nativeQuery = true)
    SiteTable getSiteStatusType(int i);

    @Query(value = "SELECT last_error FROM search_engine.site where id = :i", nativeQuery = true)
    SiteTable getLastError(int i);
}
