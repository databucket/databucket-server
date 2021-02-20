package pl.databucket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.View;

import java.util.List;

@Repository
public interface ViewRepository extends JpaRepository<View, Long> {

    View findByIdAndDeleted(long id, boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    List<View> findAllByDeletedOrderById(boolean deleted);
}
