package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteTable;

@Repository
public interface SiteRepository extends JpaRepository<SiteTable, Integer> {

}
