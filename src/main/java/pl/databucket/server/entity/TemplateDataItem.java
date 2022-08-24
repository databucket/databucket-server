package pl.databucket.server.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Map;

@Entity
@Getter
@Setter
@Table(name="templates_data_items")
public class TemplateDataItem extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "data_item_generator")
    @SequenceGenerator(name="data_item_generator", sequenceName = "data_item_seq", allocationSize = 1)
    @Column(name = "item_id", updatable = false, nullable = false)
    private long id;

    @OneToOne
    @JoinColumn(name = "data_id", referencedColumnName = "data_id")
    private TemplateData templateData;

    @Column(name = "tag_uid")
    private String tagUid;

    @Column
    private boolean reserved;

    @Column(name = "reserved_by")
    private String owner;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> properties;

}
