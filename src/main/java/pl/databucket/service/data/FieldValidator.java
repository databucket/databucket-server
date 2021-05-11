package pl.databucket.service.data;

import pl.databucket.exception.EmptyInputValueException;
import pl.databucket.exception.IncorrectValueException;

import java.util.*;
import java.util.regex.Pattern;

public class FieldValidator {

    public static void validateSort(String sort) throws IncorrectValueException {
        if (sort.startsWith("asc(") || sort.startsWith("desc(")) {
            final Pattern pattern = Pattern.compile("[asc|desc]+\\(.+\\)");
            if (!pattern.matcher(sort).matches()) {
                throw new IncorrectValueException("Invalid sorting. The sort param must equals a field or asc(field) or desc(field). By default is used ascending sorting. As a filed can be set column name or json properties path.");
            }
        }
    }

    public static List<Condition> validateListOfConditions(List<Map<String, Object>> conditionsMap, boolean obligatory) throws EmptyInputValueException {
        if (conditionsMap != null) {
            List<Condition> conditions = new ArrayList<>();
            for (Map<String, Object> conditionMap : conditionsMap)
                conditions.add(new Condition(conditionMap));
            return conditions;
        } else {
            if (obligatory)
                throw new EmptyInputValueException(COL.CONDITIONS);
            else
                return null;
        }
    }

//    public static List<CustomColumnDto> validateListOfColumns(List<CustomColumnDto> columnsMap) throws EmptyInputValueException {
//        return columnsMap.stream().map(c -> {
//            return new HashMap<String, String>() {{
//                put("field", convertField(columnDef.get("field")));
//                put("title", columnDef.get("title"));
//            }};
//        }).collect(Collectors.toList());
//    }
//
//    private String convertField(String fieldName) {
//        return fieldName;
//    }

    public static void mustBeGraterThen0(String fieldName, Integer value) throws IncorrectValueException {
        if (value <= 0)
            throw new IncorrectValueException("The '" + fieldName + "' must be grater then 0!");

    }

    public static void mustBeGraterOrEqual0(String fieldName, Integer value) throws IncorrectValueException {
        if (value < 0)
            throw new IncorrectValueException("The '" + fieldName + "' must be grater or equal to 0!");

    }

}
