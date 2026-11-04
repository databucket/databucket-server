package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.databucket.server.entity.UsedSsoCode;

import java.util.Date;

@Repository
public interface UsedSsoCodeRepository extends JpaRepository<UsedSsoCode, String> {

    @Transactional
    @Modifying
    @Query("delete from UsedSsoCode c where c.expiresAt < :now")
    int deleteExpired(@Param("now") Date now);
}
