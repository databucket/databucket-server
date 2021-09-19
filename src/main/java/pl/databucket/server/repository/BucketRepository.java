package pl.databucket.server.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.DataClass;

import java.util.List;

@Repository
public interface BucketRepository extends JpaRepository<Bucket, Long> {

    Bucket findByIdAndDeleted(long id, boolean deleted);
    Bucket findByNameAndDeleted(String name, boolean deleted);
    List<Bucket> findAllByDeletedAndIdIn(boolean deleted, Iterable<Long> longs);
    List<Bucket> findAllByDeletedOrderById(boolean deleted);
    List<Bucket> findAllByDeletedAndDataClass(boolean deleted, DataClass dataClassId);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    Page<Bucket> findAll(Specification<Bucket> specification, Pageable pageable);
}
