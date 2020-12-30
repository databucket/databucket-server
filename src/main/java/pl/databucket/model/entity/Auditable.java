package pl.databucket.model.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;


@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class Auditable<U>
{
    @CreatedBy
    @Column(name = "created_by", length = 50)
    private U createdBy;

    @CreatedDate
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private U updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
}
