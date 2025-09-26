package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index,Integer> {
    @Query("SELECT COUNT(i) FROM Index i WHERE i.lemma.id = :id")
    Long countPageToLemma(@Param("id") Integer id);

    @Query("SELECT p FROM Index i INNER JOIN i.page p WHERE i.lemma.lemma = :lemma")
    List<Page> findPagesByLemma(@Param("lemma") String lemma);

    @Query("SELECT i FROM Index i WHERE i.lemma.id IN :lemmaIds")
    List<Index> findByLemmaIdIn(@Param("lemmaIds") List<Integer> lemmaIds);

}
