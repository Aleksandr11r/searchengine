package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Entity
@Getter
@Setter
@Table(name = "page", indexes = @jakarta.persistence.Index(columnList = "path"))
@NoArgsConstructor
public class Page {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (nullable = false, name = "id")
    private Integer id;
    @Column (name = "path", columnDefinition = "TEXT")
    private String path;

    @Column (name = "code")
    private Integer code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @OneToMany(mappedBy = "page", cascade = CascadeType.REMOVE,fetch = FetchType.LAZY)
    private List<Index> indexList;

    public Page(String path) {
        this.path = path;
    }
}
