package pl.databucket.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.util.Date;

/**
 * An SSO code (its jti) that has already been exchanged for a token. Kept in the database, not in memory,
 * so a code can't be used twice also when Databucket runs as several replicas. Rows are removed once the code expires.
 */
@Entity
@NoArgsConstructor
@Getter
@Table(name = "used_sso_code")
public class UsedSsoCode implements Persistable<String> {

    @Id
    @Column(name = "code_id", length = 64, updatable = false, nullable = false)
    private String id;

    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;

    @Transient
    private boolean isNew = true;

    public UsedSsoCode(String id, Date expiresAt) {
        this.id = id;
        this.expiresAt = expiresAt;
    }

    /** A new row is inserted (never merged), so a second use of the same code fails on the primary key. */
    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }
}
