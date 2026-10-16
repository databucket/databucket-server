package pl.databucket.server.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.stream.Collectors;


@Service
@Transactional
public class TagService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    public TagDto createTag(TagDto tagDto) throws ItemAlreadyExistsException {
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

        modelMapper.map(tagRepository.save(tag), tagDto);
        return tagDto;
    }

    @Transactional(readOnly = true)
    public List<TagDto> getTags() {
        return tagRepository.findAllByDeletedOrderById(false).stream()
                .map(item -> modelMapper.map(item, TagDto.class))
                .collect(Collectors.toList());
    }

    public TagDto modifyTag(TagDto tagDto) throws ItemNotFoundException, ItemAlreadyExistsException, ModifyByNullEntityIdException {
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

        modelMapper.map(tagRepository.save(tag), tagDto);
        return tagDto;
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
