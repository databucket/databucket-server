package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.TagDto;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.DataClass;
import pl.databucket.server.entity.Tag;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.BucketRepository;
import pl.databucket.server.repository.DataClassRepository;
import pl.databucket.server.repository.TagRepository;

import java.util.HashSet;
import java.util.List;


@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    public Tag createTag(TagDto tagDto) throws ItemAlreadyExistsException {
        if (tagRepository.existsByNameAndDeleted(tagDto.getName(), false))
            throw new ItemAlreadyExistsException(Tag.class, tagDto.getName());

        Tag tag = new Tag();
        tag.setName(tagDto.getName());
        tag.setDescription(tagDto.getDescription());

        if (tagDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, tagDto.getBucketsIds());
            tag.setBuckets(new HashSet<>(buckets));
        }

        if (tagDto.getClassesIds() != null) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, tagDto.getClassesIds());
            tag.setDataClasses(new HashSet<>(dataClasses));
        }

        return tagRepository.save(tag);
    }

    public List<Tag> getTags() {
        return tagRepository.findAllByDeletedOrderById(false);
    }

    public Tag modifyTag(TagDto tagDto) throws ItemNotFoundException, ItemAlreadyExistsException, ModifyByNullEntityIdException {
        if (tagDto.getId() == null)
            throw new ModifyByNullEntityIdException(Tag.class);

        Tag tag = tagRepository.findByIdAndDeleted(tagDto.getId(), false);

        if (tag == null)
            throw new ItemNotFoundException(Tag.class, tag.getId());

        if (!tag.getName().equals(tagDto.getName()))
            if (tagRepository.existsByNameAndDeleted(tagDto.getName(), false))
                throw new ItemAlreadyExistsException(Tag.class, tagDto.getName());

        tag.setName(tagDto.getName());
        tag.setDescription(tagDto.getDescription());

        if (tagDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, tagDto.getBucketsIds());
            tag.setBuckets(new HashSet<>(buckets));
        } else
            tag.setBuckets(null);

        if (tagDto.getClassesIds() != null) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, tagDto.getClassesIds());
            tag.setDataClasses(new HashSet<>(dataClasses));
        } else
            tag.setDataClasses(null);

        return tagRepository.save(tag);
    }

    public void deleteTag(long tagId) throws ItemNotFoundException {
        Tag tag = tagRepository.findByIdAndDeleted(tagId, false);

        if (tag == null)
            throw new ItemNotFoundException(Tag.class, tagId);

        tag.setBuckets(null);
        tag.setDataClasses(null);

        tag.setDeleted(true);
        tagRepository.save(tag);
    }
}
