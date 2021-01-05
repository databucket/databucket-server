package pl.databucket.specification;

import net.kaczmarzyk.spring.data.jpa.domain.*;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.jpa.domain.Specification;
import pl.databucket.configuration.Constants;
import pl.databucket.entity.Columns;
import pl.databucket.entity.Tag;

@And({
    @Spec(path = "id", spec = In.class, paramSeparator = ','),
    @Spec(path = "name", spec = Like.class),
    @Spec(path = "description", spec = Like.class),
    @Spec(path = "createdBy", spec = In.class, paramSeparator = ','),
    @Spec(path = "createdDate", params={"createdAfter"}, spec = GreaterThanOrEqual.class, config = Constants.DATE_FORMAT),
    @Spec(path = "createdDate", params={"createdBefore"}, spec = LessThanOrEqual.class, config = Constants.DATE_FORMAT),
    @Spec(path = "lastModifiedBy", spec = In.class, paramSeparator = ','),
    @Spec(path = "lastModifiedDate", params={"modifiedAfter"}, spec = GreaterThanOrEqual.class, config = Constants.DATE_FORMAT),
    @Spec(path = "lastModifiedDate", params={"modifiedBefore"}, spec = LessThanOrEqual.class, config = Constants.DATE_FORMAT),
    @Spec(path = "deleted", constVal = "false", spec = Equal.class)
})
public interface TagSpecification extends Specification<Tag> {
}