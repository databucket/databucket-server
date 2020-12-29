package pl.databucket.database;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import pl.databucket.exception.ConditionNotAllowedException;
import pl.databucket.exception.UnknownColumnException;

public class Query {

	String query;
	String table;
	String alias;
	Boolean withAlias;

	public Query(String table, boolean withAlias) {
		this.table = table;
		this.withAlias = withAlias;
		switch (table) {
		case TAB.BUCKET:
			alias = "b";
			break;
		case TAB.COLUMNS:
			alias = "c";
			break;
		case TAB.FILTER:
			alias = "f";
			break;
		case TAB.VIEW:
			alias = "v";
			break;
		case TAB.TAG:
			alias = "t";
			break;
		default:
			alias = "a";
		}
	}

	public Query selectCount() {
		query = "SELECT count(*)";
		return this;
	}

	public Query select(String column) throws UnknownColumnException {
		if (withAlias && !column.equals(COL.COUNT))
			query = "SELECT " + getFieldWithAlias(column, false);
		else
			query = "SELECT " + column;

		return this;
	}

	public Query select(String[] columns) throws UnknownColumnException {
		query = "SELECT " + columnsToString(columns);
		return this;
	}

	public Query selectBundles(List<Map<String, Object>> columns) throws UnknownColumnException {
		query = "SELECT " + columnsToString(columns);
		return this;
	}

	public Query delete() {
		query = "DELETE";

		if (withAlias)
			query += " " + alias;

		return this;
	}

	public Query update() {
		if (withAlias)
			query = "UPDATE \"" + table + "\" " + alias;
		else
			query = "UPDATE \"" + table + "\"";

		return this;
	}

	public Query set(Map<String, Object> paramMap) {
		if (!paramMap.isEmpty())
			query += " SET " + columnsToSetString(paramMap);
		return this;
	}

	public Query setWithValues(Map<String, Object> paramMap) {
		if (!paramMap.isEmpty())
			query += " SET " + columnsToSetStringWithValues(paramMap);
		return this;
	}

	private String getJsonPathForMerge(String nestedPath) {
		String[] pathSteps = nestedPath.split("\\.");
		if (pathSteps.length > 1)
			return "\"" + pathSteps[0] + "\": {"
					+ getJsonPathForMerge(nestedPath.substring(nestedPath.indexOf(".") + 1)) + "}";
		else
			return "\"" + nestedPath + "\": {}";
	}

	public Query removeAndSetProperties(boolean execute, Map<String, Object> payload) {
		if (execute) {
			List<String> remProperties = (List<String>) payload.get(C.REMOVE_PROPS);
			Map<String, Object> setProperties = (Map<String, Object>) payload.get(C.UPDATE_PROPS);
			if (remProperties != null && remProperties.size() > 0 || setProperties != null && setProperties.size() > 0) {
				if (query.contains("SET"))
					query += ", properties = properties ";
				else
					query += " properties = properties ";


				if (remProperties != null && remProperties.size() > 0) {
					for (String prop : remProperties)
						query += "-'" + prop.substring(2) + "'";
				}

				if (setProperties != null && setProperties.size() > 0) {
					query += "|| '";
					DocumentContext dc = JsonPath.parse("{}");

					for (Map.Entry<String, Object> prop : setProperties.entrySet()) {
						String key = prop.getKey();
						int lastDot = key.lastIndexOf(".");
						String path = key.substring(0, lastDot);
						String name = key.substring(lastDot + 1);
						dc.put(path, name, prop.getValue());
					}

					query += dc.jsonString() + "'";
				}
			}
		}

		return this;
	}

	public Query joinTags(boolean doIt) {
		if (doIt)
			query += " LEFT OUTER JOIN \"_tag\" t ON t.tag_id = a.tag_id";
		return this;
	}

	public Query insertIntoValues(MapSqlParameterSource mapParameters) {
		query = "INSERT INTO \"" + table + "\"" + intoValues(mapParameters);
		return this;
	}

	public Query dropTable() {
		query = "DROP TABLE \"" + table + "\"";
		return this;
	}

	public Query from() {
		if (withAlias)
			query += " FROM \"" + table + "\" " + alias;
		else
			query += " FROM \"" + table + "\"";
		return this;
	}

	public Query where(Condition condition, Map<String, Object> paramMap) throws UnknownColumnException, ConditionNotAllowedException {
		if (condition != null) {
			query += " WHERE " + generateConditionString("n", condition, paramMap);
		}
		return this;
	}

	public void where(Condition condition) throws UnknownColumnException, ConditionNotAllowedException {
		if (condition != null) {
			query += " WHERE " + generateConditionString(null, condition, null);
		}
	}

	public Query where(List<Condition> conditions, Map<String, Object> paramMap) throws UnknownColumnException, ConditionNotAllowedException {
		if (conditions != null) {
			if (conditions.size() > 1) {
				String condStr = "";
				for (int i = 0; i < conditions.size(); i++) {
					condStr += generateConditionString("n" + i, conditions.get(i), paramMap) + " and ";
				}
				query += " WHERE " + condStr.substring(0, condStr.length() - " and ".length());
			} else if (conditions.size() > 0)
				where(conditions.get(0), paramMap);
		}
		return this;
	}

	public Query where(List<Condition> conditions) throws UnknownColumnException, ConditionNotAllowedException {
		if (conditions != null) {
			if (conditions.size() > 1) {
				String condStr = "";
				for (Condition condition : conditions) {
					condStr += generateConditionString(null, condition, null) + " and ";
				}
				query += " WHERE " + condStr.substring(0, condStr.length() - " and ".length());
			} else if (conditions.size() > 0)
				where(conditions.get(0));
		}
		return this;
	}

	private String getConditionStringValue(String uniqueName, SourceType sourceType, Object value,	Map<String, Object> paramMap) throws UnknownColumnException {
		if (sourceType.equals(SourceType.s_function)) {
			String sValue = (String) value;
			if (sValue.startsWith("$.") && sValue.endsWith(")")) {
				int lastDot = sValue.lastIndexOf('.');
				String fName = sValue.substring(0, lastDot);
				String functionName = sValue.substring(lastDot + 1);
				switch (functionName) {
					case "length()":
						return "jsonb_array_length(a.properties #> '{" + getPGPropertyArray(fName) + "}')";
					case "isNotNull()":
						return "(a.properties #>> '{" + getPGPropertyArray(fName) + "}' is not null)";
					case "isNull()":
						return "(a.properties #>> '{" + getPGPropertyArray(fName) + "}' is null)";
					case "notExists()":
						return "(a.properties #> '{" + getPGPropertyArray(fName) + "}' is null)";
					case "exists()":
						return "(a.properties #> '{" + getPGPropertyArray(fName) + "}' is not null)";
					default:
						return sValue;
				}
			} else
				return sValue;
		} else if (sourceType.equals(SourceType.s_property)) {
			if (withAlias)
				return "a.properties #> '" + getPGPropertyArray((String) value) + "'";
			else
				return "properties #> '" + getPGPropertyArray((String) value) + "'";
		} else if (sourceType.equals(SourceType.s_field)) {
			if (withAlias) {
				return getFieldWithAlias((String) value, false);
			} else {
				return (String) value;
			}
		} else if (sourceType.equals(SourceType.s_const)) {
			if (paramMap != null) {
				if (value instanceof String) {
					Timestamp date = isValidDate((String) value);
					if (date != null)
						paramMap.put(uniqueName, date);
					else
						paramMap.put(uniqueName, value);
				} else
					paramMap.put(uniqueName, value);

				return ":" + uniqueName;
			} else {
				if (value instanceof String) {
					return "'" + value + "'";
				} else
					return "" + value;
			}
		}

		return null;
	}

	private String generateConditionString(String uniqueName, Condition condition, Map<String, Object> paramMap) throws UnknownColumnException, ConditionNotAllowedException {
		String sFormat = "%s %s %s";
		String v1;
		String op;
		String v2;

		if (condition.getOperator().equals(Operator.in) || condition.getOperator().equals(Operator.notIn)) {
			if (condition.getOperator().equals(Operator.notIn))
				sFormat = "NOT(%s %s %s)";

			// eg.: $.textValue in $.textArray
			if (condition.getLeftSource().equals(SourceType.s_property) && condition.getRightSource().equals(SourceType.s_property)) {
				throw new ConditionNotAllowedException(condition);

			// eg.: 5 in $.jsonArray   >>> v1 @> '[v2]'
			} else if (condition.getRightSource().equals(SourceType.s_property)) {
				v1 = "a.properties #> '{" + getPGPropertyArray(condition.getRightValue().toString()) + "}'";
				op = "@>";
				if (condition.getLeftValue() instanceof String) {
					v2 = "'[\"" + condition.getLeftValue() + "\"]'";
				} else if (condition.getLeftValue() instanceof ArrayList<?>) {
					v2 = "'[" + getPGStringArray((ArrayList<?>) condition.getLeftValue()) + "]'";
				} else
					v2 = "'[" + condition.getLeftValue() + "]'";

			// eg.: $.prop in [1, 2, 3]   >>> v1 in 'v2
			} else if (condition.getLeftSource().equals(SourceType.s_property)) {
				if (condition.getRightValue() instanceof ArrayList)
					v1 = "'[" + getPGStringArray((ArrayList<?>) condition.getRightValue()) + "]'";
				else {
					if (condition.getRightValue() instanceof String) {
						v1 = "'[\"" + condition.getRightValue() + "\"]'";
					} else {
						v1 = "'[" + condition.getRightValue() + "]'";
					}
				}
				op = "@>";
				v2 = "(a.properties #> '{" + getPGPropertyArray(condition.getLeftValue().toString()) + "}')";

			// property not used in this condition
			} else {
				sFormat = "%s %s (%s)";
				if (paramMap != null) {
					v1 = getConditionStringValue(uniqueName + "1", condition.getLeftSource(), condition.getLeftValue(), paramMap);
					op = condition.getOperator().toString();
					v2 = getConditionStringValue(uniqueName + "2", condition.getRightSource(), condition.getRightValue(), paramMap);
				} else {
					v1 = getConditionStringValue(null, condition.getLeftSource(), condition.getLeftValue(), null);
					op = condition.getOperator().toString();
					v2 = getConditionStringValue(null, condition.getRightSource(), condition.getRightValue(), null);
				}
			}

		// LIKE, NOT LIKE, SIMILAR TO, NOT SIMILAR TO, MATCHES, NOT MATCH operator
		} else if (condition.getOperator().equals(Operator.like)
				|| condition.getOperator().equals(Operator.notLike)
				|| condition.getOperator().equals(Operator.similarTo)
				|| condition.getOperator().equals(Operator.notSimilarTo)
				|| condition.getOperator().equals(Operator.matchesCI)
				|| condition.getOperator().equals(Operator.notMatchCI)
				|| condition.getOperator().equals(Operator.matchesCS)
				|| condition.getOperator().equals(Operator.notMatchCS)) {

			String leftValue, rightValue;

			if (condition.getLeftSource().equals(SourceType.s_property))
				leftValue = "a.properties #>> '{" + getPGPropertyArray((String) condition.getLeftValue()) + "}'";
			else
				leftValue = "(a." + condition.getLeftValue() + ")::varchar";

			if (condition.getRightSource().equals(SourceType.s_property))
				rightValue = "a.properties #>> '{" + getPGPropertyArray((String) condition.getRightValue()) + "}'";
			else
				rightValue = "(a." + condition.getRightValue() + ")::varchar";

			if (paramMap != null) {
				v1 = getConditionStringValue(uniqueName + "1", condition.getLeftSource(), leftValue, paramMap);
				op = condition.getOperator().toString();
				v2 = getConditionStringValue(uniqueName + "2", condition.getRightSource(), rightValue, paramMap);
			} else {
				v1 = getConditionStringValue(null, condition.getLeftSource(), leftValue, null);
				op = condition.getOperator().toString();
				v2 = getConditionStringValue(null, condition.getRightSource(), rightValue, null);
			}
		} else {
			if (paramMap != null) {
				v1 = getConditionStringValue(uniqueName + "1", condition.getLeftSource(), condition.getLeftValue(), paramMap);
				op = condition.getOperator().toString();
				if (condition.getLeftSource().equals(SourceType.s_property) && condition.getRightValue() instanceof Boolean)
					v2 = Boolean.toString((boolean) condition.getRightValue());
				else
					v2 = getConditionStringValue(uniqueName + "2", condition.getRightSource(), condition.getRightValue(),
							paramMap);
			} else {
				v1 = getConditionStringValue(null, condition.getLeftSource(), condition.getLeftValue(), null);
				op = condition.getOperator().toString();
				if (condition.getLeftSource().equals(SourceType.s_property) && condition.getRightValue() instanceof Boolean)
					v2 = Boolean.toString((boolean) condition.getRightValue());
				else
					v2 = getConditionStringValue(null, condition.getRightSource(), condition.getRightValue(), null);
			}
		}

		return String.format(sFormat, v1, op, v2);
	}

	private String getFieldWithAlias(String fieldName, boolean addAs) throws UnknownColumnException {
		switch (table) {
		case TAB.BUCKET:
			return "b." + fieldName;
		case TAB.COLUMNS:
			return "c." + fieldName;
		case TAB.FILTER:
			return "f." + fieldName;
		case TAB.VIEW:
			return "v." + fieldName;
		case TAB.TAG:
			return "t." + fieldName;
		default:
			if (table.endsWith("_history")) 
				return "a." + fieldName;
			
			if (fieldName.startsWith("$.")) {
				if (fieldName.endsWith(")")) {
					int lastDot = fieldName.lastIndexOf('.');
					String filedJsonPath = fieldName.substring(0, lastDot);
					String functionName = fieldName.substring(lastDot + 1);
					String resultFiled;

					switch (functionName) {
						case "length()":
							resultFiled = "jsonb_array_length(a.properties #> '{" + getPGPropertyArray(filedJsonPath) + "}')";
							break;

						case "isNotNull()":
							resultFiled = "a.properties #>> '{" + getPGPropertyArray(filedJsonPath) + "}' is not null";
							break;

						case "isNull()":
							resultFiled = "a.properties #>> '{" + getPGPropertyArray(filedJsonPath) + "}' is null";
							break;

						case "notExists()":
							resultFiled = "a.properties #> '{" + getPGPropertyArray(filedJsonPath) + "}' is null";
							break;

						case "exists()":
							resultFiled = "a.properties #> '{" + getPGPropertyArray(filedJsonPath) + "}' is not null";
							break;

						default:
							throw new UnknownColumnException(fieldName);
					}

					if (addAs)
						return resultFiled + " as \"" + fieldName + "\"";
					else
						return resultFiled;

				} else {
					if (addAs)
						return "a.properties #>> '{" + getPGPropertyArray(fieldName) + "}' as \"" + fieldName + "\"";
					else
						return "a.properties #>> '{" + getPGPropertyArray(fieldName) + "}'";
				}
			} else {
				switch (fieldName) {
				case COL.TAG_NAME:
					return "t." + fieldName;
				case COL.DATA_ID:
				case COL.TAG_ID:
				case COL.LOCKED:
				case COL.LOCKED_BY:
				case COL.CREATED_AT:
				case COL.CREATED_BY:
				case COL.UPDATED_AT:
				case COL.UPDATED_BY:
				case COL.PROPERTIES:
					return "a." + fieldName;
				default:
					return fieldName.replace(COL.PROPERTIES, "a." + COL.PROPERTIES)
							.replace(COL.TAG_NAME, "t." + COL.TAG_NAME)
							.replace(COL.DATA_ID, "a." + COL.DATA_ID)
							.replace(COL.TAG_ID, "a." + COL.TAG_ID)
							.replace(COL.LOCKED, "a." + COL.LOCKED)
							.replace(COL.LOCKED_BY, "a." + COL.LOCKED_BY)
							.replace(COL.CREATED_AT, "a." + COL.CREATED_AT)
							.replace(COL.CREATED_BY, "a." + COL.CREATED_BY)
							.replace(COL.UPDATED_AT, "a." + COL.UPDATED_AT)
							.replace(COL.UPDATED_BY, "a." + COL.UPDATED_BY);
				}
			}
		}
	}

	private String getPGPropertyArray(String jsonPath) {
		// $.group.subgroup.item 	>>>>  group,subgroup,item
		String result = jsonPath.substring(2);
		result = result.replace(".", ",");
		return result;
	}

	// returns eg.: "text1","text2","text3"
	private String getPGStringArray(ArrayList<?> list) {
		StringBuilder result = new StringBuilder();
		for(Object item : list)
			if (item instanceof String)
				result.append(",\"").append(item).append("\"");
			else
				result.append(",").append(item);
		return result.substring(1); // cut first comma character
	}

	private java.sql.Timestamp isValidDate(String inDate) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		dateFormat.setLenient(false);
		try {
			return new java.sql.Timestamp(dateFormat.parse(inDate.trim()).getTime());
		} catch (ParseException pe) {
			return null;
		}
	}

	public Query orderBy(Optional<String> sort) throws UnknownColumnException {
		if (sort.isPresent())
			return orderBy(sort.get());
		else
			return this;
	}

	public Query orderBy(String sort) throws UnknownColumnException {
		boolean asc = true;
		String column = sort;

		if (sort.startsWith("desc(")) {
			column = column.replace("desc(", "");
			column = column.substring(0, column.length() - 1);
			asc = false;
		} else if (sort.startsWith("asc(")) {
			column = column.replace("asc(", "");
			column = column.substring(0, column.length() - 1);
		}

		if (column.toUpperCase().startsWith("RANDOM()"))
			orderBy("RANDOM()", true);
		else
			orderBy(getFieldWithAlias(column, false), asc);

		return this;
	}

	public Query orderBy(String columnName, boolean asc) {
		if (asc) {
			query += " ORDER BY " + columnName;
		} else {
			query += " ORDER BY " + columnName + " desc";
		}
		return this;
	}

	public Query limit(Map<String, Object> paramMap, int limit) {
		query += " LIMIT :limit";
		paramMap.put("limit", limit);
		return this;
	}

	public Query limitPage(Map<String, Object> paramMap, Optional<Integer> limit, Optional<Integer> page) {
		if (limit.isPresent())
			limit(paramMap, limit.get());

		if (page.isPresent())
			offset(paramMap, page.get() * limit.get() - limit.get());
		return this;
	}

	public void offset(Map<String, Object> paramMap, int offset) {
		query += " OFFSET :offset";
		paramMap.put("offset", offset);
	}

	public String toString(Logger logger, Map<String, Object> paramMap) {
		if (paramMap != null) {
			logger.debug("paramMap: " + convertWithStream(paramMap));
		}
		logger.debug(query);
		return query;
	}

	public String convertWithStream(Map<String, Object> map) {
		String mapAsString = map.keySet().stream()
				.map(key -> key + "=" + map.get(key))
				.collect(Collectors.joining(", ", "{", "}"));
		return mapAsString;
	}

	private String intoValues(MapSqlParameterSource mapParameters) {
		String columns = "";
		String values = "";
		for (Map.Entry<String, Object> prop : mapParameters.getValues().entrySet()) {
			columns += ", \"" + prop.getKey() + "\"";
			values += ", :" + prop.getKey();
		}
		return " (" + columns.substring(2) + ") VALUES (" + values.substring(2) + ")";
	}

	private String columnsToString(String[] columns) throws UnknownColumnException {
		String result = "";
		for (String col : columns)
			result += ", " + getFieldWithAlias(col, true);
		return result.substring(2);
	}

	private String columnsToString(List<Map<String, Object>> columns) throws UnknownColumnException {
		String result = "";
		for (Map<String, Object> col : columns)
			if (col.containsKey(C.TITLE))
				result += ", " + getFieldWithAlias((String) col.get(C.FIELD), false) + " as \"" + col.get(C.TITLE)+ "\"";
			else
				result += ", " + getFieldWithAlias((String) col.get(C.FIELD), false);
		return result.substring(2);
	}

	private String columnsToSetString(Map<String, Object> paramMap) {
		String result = "";
		for (Map.Entry<String, Object> col : paramMap.entrySet())
			if (withAlias)
				result += ", " + alias + "." + col.getKey() + " = :" + col.getKey();
			else
				result += ", " + col.getKey() + " = :" + col.getKey();
		return result.substring(2);
	}

	private String columnsToSetStringWithValues(Map<String, Object> paramMap) {
		String result = "";
		for (Map.Entry<String, Object> col : paramMap.entrySet())
			if (withAlias) {
				if (col.getValue() instanceof String)
					result += ", " + alias + "." + col.getKey() + " = \"" + col.getValue() + "\"";
				else
					result += ", " + alias + "." + col.getKey() + " = " + col.getValue();
			} else {
				if (col.getValue() instanceof String)
					result += ", " + col.getKey() + " = \"" + col.getValue() + "\"";
				else
					result += ", " + col.getKey() + " = " + col.getValue();
			}
		return result.substring(2);
	}

}
