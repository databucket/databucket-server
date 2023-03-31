package pl.databucket.server.service;

import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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


@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final BucketRepository bucketRepository;
    private final DataClassRepository dataClassRepository;
    private final ModelMapper modelMapper;

    public TagDto createTag(TagDto tagDto) throws ItemAlreadyExistsException {
        if (tagRepository.existsByNameAndDeleted(tagDto.getName(), false)) {
            throw new ItemAlreadyExistsException(Tag.class, tagDto.getName());
        }
        Tag tag = new Tag();
        modelMapper.map(tagDto, tag);

        if (tagDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, tagDto.getBucketsIds());
            tag.setBuckets(new HashSet<>(buckets));
        }

        if (tagDto.getClassesIds() != null) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, tagDto.getClassesIds());
            tag.setDataClasses(new HashSet<>(dataClasses));
        }

        Tag newTag = tagRepository.save(tag);
        return modelMapper.map(newTag, TagDto.class);
    }

    public List<TagDto> getTags() {
        return tagRepository.findAllByDeletedOrderById(false).stream()
            .map(item -> modelMapper.map(item, TagDto.class)).toList();
    }

    public TagDto modifyTag(TagDto tagDto)
        throws ItemNotFoundException, ItemAlreadyExistsException, ModifyByNullEntityIdException {
        if (tagDto.getId() == null) {
            throw new ModifyByNullEntityIdException(Tag.class);
        }

        Tag tag = tagRepository.findByIdAndDeleted(tagDto.getId(), false);

        if (tag == null) {
            throw new ItemNotFoundException(Tag.class, tagDto.getId());
        }

        if (!tag.getName().equals(tagDto.getName()) && (tagRepository.existsByNameAndDeleted(tagDto.getName(),
            false))) {
            throw new ItemAlreadyExistsException(Tag.class, tagDto.getName());

        }

        modelMapper.map(tagDto, tag);

        if (tagDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, tagDto.getBucketsIds());
            tag.setBuckets(new HashSet<>(buckets));
        } else {
            tag.setBuckets(null);
        }

        if (tagDto.getClassesIds() != null) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, tagDto.getClassesIds());
            tag.setDataClasses(new HashSet<>(dataClasses));
        } else {
            tag.setDataClasses(null);
        }

        Tag updated = tagRepository.save(tag);
        return modelMapper.map(updated, TagDto.class);
    }

    public void deleteTag(long tagId) throws ItemNotFoundException {
        Tag tag = tagRepository.findByIdAndDeleted(tagId, false);

        if (tag == null) {
            throw new ItemNotFoundException(Tag.class, tagId);
        }

        tag.setBuckets(null);
        tag.setDataClasses(null);

        tag.setDeleted(true);
        tagRepository.save(tag);
    }
}
