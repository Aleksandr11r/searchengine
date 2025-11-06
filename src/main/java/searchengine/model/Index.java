package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@NoArgsConstructor
@Table(name = "indexes")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "id", nullable = false)
    private Integer id;

    @Column (name = "`rank`", nullable = false)
    private Float rank;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "page_id", nullable = false)
    private Page page;

    @ManyToOne (fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;
}
