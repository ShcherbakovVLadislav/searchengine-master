package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "search_index")
@Data
@NoArgsConstructor
public class SearchIndex {
    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @NotNull
    @Column(name = "page_id")
    private int pageId;
    @NotNull
    @Column(name = "lemma_id")
    private int lemmaId;
    @NotNull
    private float lemmaCount;
    @ManyToOne
    @JoinColumn(name = "page_id",insertable = false,updatable = false,nullable = false)
    private SitesPageTable sitesPageTable;
    @ManyToOne
    @JoinColumn(name = "lemma_id",insertable = false,updatable = false,nullable = false)
    private Lemma lemma;
}
