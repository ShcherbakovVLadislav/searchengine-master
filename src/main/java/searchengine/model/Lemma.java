package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "lemma", uniqueConstraints = @UniqueConstraint(columnNames = "lemma"))
@Data
@NoArgsConstructor
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    private int id;

    @NotNull
    private int frequency;

    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String lemma;

    @NotNull
    @Column(name = "site_id")
    private int siteId;

    @ManyToOne
    @JoinColumn(name = "site_id", insertable = false, updatable = false, nullable = false)
    private SiteTable siteTable;

}
