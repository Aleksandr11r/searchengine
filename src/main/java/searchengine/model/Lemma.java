package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

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

    @OneToMany(mappedBy = "lemma", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Index> indexList;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Lemma that = (Lemma) obj;
        return Objects.equals(this.lemma, that.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemma);
    }

}
