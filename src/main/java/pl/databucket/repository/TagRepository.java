package pl.databucket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Tag;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Tag findByIdAndDeleted(long id, boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    List<Tag> findAllByDeletedOrderById(boolean deleted);
}
