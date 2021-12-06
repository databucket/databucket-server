package pl.databucket.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import pl.databucket.server.exception.UnexpectedException;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

public class ServiceUtils {

    public void convertPropertiesColumns(List<Map<String, Object>> source) {
        for (Map<String, Object> map : source) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue() instanceof Timestamp) {
                        entry.setValue(((Timestamp) entry.getValue()).toInstant().toString());
                    } else if (entry.getValue() instanceof PGobject) {
                        String value = ((PGobject) entry.getValue()).getValue();
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            entry.setValue(value.substring(1, value.length() - 1));
                        } else if (value.equals("true")) {
                            entry.setValue(true);
                        } else if (value.equals("false")) {
                            entry.setValue(false);
                        } else if (value.equals("null")) {
                            entry.setValue(null);
                        } else {
                            try {
                                Integer integerValue = Integer.parseInt(value);
                                entry.setValue(integerValue);
                            } catch (NumberFormatException e1) {
                                try {
                                    Double doubleValue = Double.parseDouble(value);
                                    entry.setValue(doubleValue);
                                } catch (NumberFormatException e2) {
                                    // do nothing
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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
