package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Bucket;

@Repository
public interface BucketRepository extends JpaRepository<Bucket, Long> {

    Bucket findByIdAndDeleted(long id, boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    Page<Bucket> findAll(Specification<Bucket> specification, Pageable pageable);
}
