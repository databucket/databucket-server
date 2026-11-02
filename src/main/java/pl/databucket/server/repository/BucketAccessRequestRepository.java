package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.BucketAccessRequest;
import pl.databucket.server.entity.User;

import java.util.List;

// requests of the signed-in project only (Hibernate tenant)
@Repository
public interface BucketAccessRequestRepository extends JpaRepository<BucketAccessRequest, Long> {

    List<BucketAccessRequest> findAllByStatusOrderByRequestedAt(BucketAccessRequest.Status status);

    long countByStatusAndBucketDeleted(BucketAccessRequest.Status status, boolean deleted);

    List<BucketAccessRequest> findAllByUserOrderByRequestedAt(User user);

    boolean existsByBucketAndUserAndStatus(Bucket bucket, User user, BucketAccessRequest.Status status);
}
