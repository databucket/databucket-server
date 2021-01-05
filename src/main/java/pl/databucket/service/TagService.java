package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.dto.TagDto;
import pl.databucket.entity.Tag;
import pl.databucket.repository.TagRepository;


@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    public Tag createTag(TagDto tagDto) {
        Tag tag = new Tag();
        tag.setName(tagDto.getName());
        tag.setDescription(tagDto.getDescription());
//        tag.setBuckets(); TODO
//        tag.setDataClasses(); TODO

        return tagRepository.save(tag);
    }

    public Page<Tag> getTags(Specification<Tag> specification, Pageable pageable) {
        return tagRepository.findAll(specification, pageable);
    }

    public Tag modifyTag(TagDto tagDto) {
        Tag tag = tagRepository.findById(tagDto.getId()).get();
        tag.setName(tagDto.getName());
        tag.setDescription(tagDto.getDescription());
//        tag.setBuckets(); TODO
//        tag.setDataClasses(); TODO
        return tagRepository.save(tag);
    }

    public void deleteTag(long tagId) {
        Tag tag = tagRepository.getOne(tagId);
        tag.setDeleted(true);
        tagRepository.save(tag);
    }

//    private String referencesTagItem(int tagId) throws UnknownColumnException, ItemDoNotExistsException, ConditionNotAllowedException {
//        final String AS_ID = " as \"id\"";
//        final String AS_NAME = " as \"name\"";
//        String result = "";
//
//        // get tag's bucket_id and class_id
//        List<Condition> conditions = new ArrayList<>();
//        conditions.add(new Condition(COL.TAG_ID, Operator.equal, tagId));
//        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
//
//        Map<String, Object> paramMap = new HashMap<>();
//
//        Query query = new Query(TAB.TAG, true)
//                .select(new String[]{COL.BUCKET_ID, COL.CLASS_ID})
//                .from()
//                .where(conditions, paramMap);
//
//        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
//
//        if (resultList.size() == 0)
//            throw new ItemDoNotExistsException("Tag", tagId);
//
//        // Get list of bucket to check the tag usage
//        Integer bucketId = (Integer) resultList.get(0).get(COL.BUCKET_ID);
//        Integer classId = (Integer) resultList.get(0).get(COL.CLASS_ID);
//        conditions = new ArrayList<>();
//        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
//        if (bucketId != null) {
//            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
//        } else if (classId != null) {
//            conditions.add(new Condition(COL.CLASS_ID, Operator.equal, classId));
//        }
//
//        query = new Query(TAB.BUCKET, true)
//                .select(new String[]{COL.BUCKET_ID + AS_ID, COL.BUCKET_NAME + AS_NAME})
//                .from()
//                .where(conditions, paramMap);
//
//        List<Map<String, Object>> bucketList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
//
//        List<Map<String, Object>> bucketNameList = new ArrayList<>();
//        conditions = new ArrayList<>();
//        conditions.add(new Condition(COL.TAG_ID, Operator.equal, tagId));
//        paramMap = new HashMap<>();
//        for (Map<String, Object> bucket : bucketList) {
//            String bucketName = (String) bucket.get("name");
//            query = new Query(bucketName, true)
//                    .select(COL.COUNT)
//                    .from()
//                    .where(conditions, paramMap);
//
//            int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);
//            if (count > 0)
//                bucketNameList.add(bucket);
//        }
//
//        if (bucketNameList.size() > 0) {
//            result += serviceUtils.getStringWithItemsNames(C.BUCKETS, bucketNameList);
//        }
//
//        if (result.length() > 0)
//            return result;
//        else
//            return null;
//    }

}
