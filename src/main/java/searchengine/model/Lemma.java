package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "lemma", length = 255)
    private String lemma;

    @Column(name = "frequency")
    private Integer frequency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @OneToMany(mappedBy = "lemma", fetch = FetchType.LAZY, orphanRemoval=true)
    private List<Index> indexList;

}
