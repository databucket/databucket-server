package pl.databucket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import pl.databucket.exception.UnexpectedException;

import java.sql.SQLException;
import java.util.*;

public class ServiceUtils {

    public void convertStringToMap(List<Map<String, Object>> source, String targetItemName) throws UnexpectedException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (Map<String, Object> itemMap : source) {
                String targetItemValueStr = (String) itemMap.get(targetItemName);
                if (targetItemValueStr != null) {
                    Map<String, Object> targetItemMap = mapper.readValue(targetItemValueStr, new TypeReference<Map<String, Object>>() {
                    });
                    itemMap.put(targetItemName, targetItemMap);
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public PGobject javaObjectToPGObject(Object object) throws JsonProcessingException, SQLException {
        String jsonObjectAsStr = new ObjectMapper().writeValueAsString(object);
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        pgObject.setValue(jsonObjectAsStr);

        return pgObject;
    }

}
