package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "page",indexes = {@Index(name = "path_index",columnList = "path")})
@NoArgsConstructor
@Setter
@Getter
public class SitesPageTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    private int id;
    @NotNull
    @Column(name = "site_id")
    private int siteId;
    @NotNull
    private String path;
    @NotNull
    private int code;
    @NotNull
    @Column(columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String content;
    @ManyToOne
    @JoinColumn(name = "site_id",nullable = false,insertable = false,updatable = false)
    private SiteTable siteTable;
}
