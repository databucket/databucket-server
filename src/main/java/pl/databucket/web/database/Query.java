package pl.databucket.web.database;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

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

	public Query groupBy(String[] columns) throws UnknownColumnException {
		query += " GROUP BY " + columnsToString(columns);
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
			query = "UPDATE `" + table + "` " + alias;
		else
			query = "UPDATE `" + table + "`";

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

	@SuppressWarnings("unchecked")
	public Query setProperties(boolean execute, Map<String, Object> payload, Map<String, Object> paramMap) {
		if (execute) {
			Map<String, Object> properties = (Map<String, Object>) payload.get(C.UPDATE_PROPS);
			if (properties != null && properties.size() > 0) {
				// merge for nested paths
				List<String> nestedPaths = new ArrayList<String>();
				for (Map.Entry<String, Object> prop : properties.entrySet()) {
					String path = prop.getKey().substring(2);
					if (path.contains(".")) {
						int lastDotIndex = path.lastIndexOf(".");
						String subPath = path.substring(0, lastDotIndex);
						if (!nestedPaths.contains(subPath))
							nestedPaths.add(subPath);
					}
				}

				if (nestedPaths.size() > 0) {
					if (!query.contains("SET"))
						query += " SET a.properties = JSON_MERGE(COALESCE(a.properties, '{}'), '{";
					else
						query += ", a.properties = JSON_MERGE(COALESCE(a.properties, '{}'), '{";

					String jsonPaths = "";
					for (String nestedPath : nestedPaths)
						jsonPaths += ", " + getJsonPathForMerge(nestedPath);

					query += jsonPaths.substring(2) + "}')";
				}

				if (!query.contains("SET"))
					query += " SET a.properties = JSON_SET(COALESCE(a.properties, '{}')";
				else
					query += ", a.properties = JSON_SET(COALESCE(a.properties, '{}')";

				int propId = 0;
				for (Map.Entry<String, Object> prop : properties.entrySet()) {
					if (prop.getValue() instanceof Boolean) {
						// Why?: Because by using paramMap the JDBCTemplace sets int values instead of
						// boolean.
						query += ", '" + prop.getKey() + "', " + prop.getValue();
					} else {
						propId += 1;
						String propName = "prop" + propId;
						query += ", '" + prop.getKey() + "', :" + propName;
						paramMap.put(propName, prop.getValue());
					}
				}

				query += ")";
			}
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public Query setPropertiesWithValues(Map<String, Object> payload) {
		Map<String, Object> properties = (Map<String, Object>) payload.get(C.UPDATE_PROPS);
		if (properties != null && properties.size() > 0) {
			// merge for nested paths
			List<String> nestedPaths = new ArrayList<String>();
			for (Map.Entry<String, Object> prop : properties.entrySet()) {
				String path = prop.getKey().substring(2);
				if (path.contains(".")) {
					int lastDotIndex = path.lastIndexOf(".");
					String subPath = path.substring(0, lastDotIndex);
					if (!nestedPaths.contains(subPath))
						nestedPaths.add(subPath);
				}
			}

			if (nestedPaths.size() > 0) {
				if (!query.contains("SET"))
					query += " SET a.properties = JSON_MERGE(COALESCE(a.properties, '{}'), '{";
				else
					query += ", a.properties = JSON_MERGE(COALESCE(a.properties, '{}'), '{";

				String jsonPaths = "";
				for (String nestedPath : nestedPaths)
					jsonPaths += ", " + getJsonPathForMerge(nestedPath);

				query += jsonPaths.substring(2) + "}')";
			}

			if (!query.contains("SET"))
				query += " SET a.properties = JSON_SET(COALESCE(a.properties, '{}')";
			else
				query += ", a.properties = JSON_SET(COALESCE(a.properties, '{}')";

			for (Map.Entry<String, Object> prop : properties.entrySet()) {
				if (!(prop.getValue() instanceof String)) {
					query += ", '" + prop.getKey() + "', " + prop.getValue();
				} else {
					query += ", '" + prop.getKey() + "', '" + prop.getValue() + "'";
				}
			}

			query += ")";
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public Query removeProperties(boolean execute, Map<String, Object> payload) {
		if (execute) {
			List<String> remove = (List<String>) payload.get(C.REMOVE_PROPS);
			if (remove != null && remove.size() > 0) {
				if (!query.contains("SET"))
					query += " SET a.properties = JSON_REMOVE(a.properties";
				else
					query += ", a.properties = JSON_REMOVE(a.properties";

				for (String prop : remove)
					query += ", '" + prop + "'";

				query += ")";
			}
		}
		return this;
	}

	public Query leftOuterJoin(String table, String alias, String column, String parentAlias, String parentColumn) {
		query += " LEFT OUTER JOIN `" + table + "` " + alias + " ON " + alias + "." + column + " = " + parentAlias + "."
				+ parentColumn;
		return this;
	}

	public Query joinTags(boolean doit) {
		if (doit)
			query += " LEFT OUTER JOIN `_tag` t ON t.tag_id = a.tag_id";
		return this;
	}

	public Query insertIntoValues(MapSqlParameterSource mapParameters) {
		query = "INSERT INTO `" + table + "`" + intoValues(mapParameters);
		return this;
	}

	public Query dropTable() {
		query = "DROP TABLE `" + table + "`";
		return this;
	}

	public Query from() {
		if (withAlias)
			query += " FROM `" + table + "` " + alias;
		else
			query += " FROM `" + table + "`";
		return this;
	}

	public Query where(Condition condition, Map<String, Object> paramMap) throws UnknownColumnException {
		if (condition != null) {
			query += " WHERE " + generateConditionString("n", condition, paramMap);
		}
		return this;
	}

	public Query where(Condition condition) throws UnknownColumnException {
		if (condition != null) {
			query += " WHERE " + generateConditionString(condition);
		}
		return this;
	}

	public Query where(List<Condition> conditions, Map<String, Object> paramMap) throws UnknownColumnException {
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

	public Query where(List<Condition> conditions) throws UnknownColumnException {
		if (conditions != null) {
			if (conditions.size() > 1) {
				String condStr = "";
				for (int i = 0; i < conditions.size(); i++) {
					condStr += generateConditionString(conditions.get(i)) + " and ";
				}
				query += " WHERE " + condStr.substring(0, condStr.length() - " and ".length());
			} else if (conditions.size() > 0)
				where(conditions.get(0));
		}
		return this;
	}

	private String getConditionStringValue(String uniqueName, SourceType sourceType, Object value,
			Map<String, Object> paramMap) throws UnknownColumnException {
		if (sourceType.equals(SourceType.s_function)) {
			String sValue = (String) value;
			if (sValue.startsWith("$.") && sValue.endsWith(")")) {
				int lastDot = sValue.lastIndexOf('.');
				String fName = sValue.substring(0, lastDot);
				String functionName = sValue.substring(lastDot + 1);
				if (functionName.equals("length()"))
					return "(JSON_LENGTH(a.properties->'" + fName + "'))";
				else if (functionName.equals("isNotNull()"))
					return "(JSON_EXTRACT(a.properties, '" + fName + "') is not null)";
				else if (functionName.equals("isNull()"))
					return "(JSON_EXTRACT(a.properties, '" + fName + "') is null)";
				else if (functionName.equals("isNotEmpty()"))
					return "(JSON_EXTRACT(properties, '" + fName + "') is not null and JSON_LENGTH(properties, '"
							+ fName + "') > 0)";
				else if (functionName.equals("isEmpty()"))
					return "(JSON_EXTRACT(properties, '" + fName + "') is null and JSON_LENGTH(properties, '" + fName
							+ "') = 0)";
				else if (functionName.startsWith("contains(")) {
					String val = functionName.substring(functionName.indexOf("(") + 1, functionName.length() - 1);
					return "(JSON_SEARCH(properties->'" + fName + "', 'one', " + val + ") is not null)";
				} else if (functionName.startsWith("notContains(")) {
					String val = functionName.substring(functionName.indexOf("(") + 1, functionName.length() - 1);
					return "(JSON_SEARCH(properties->'" + fName + "', 'one', " + val + ") is null)";
				} else
					return sValue;
			} else
				return sValue;
		} else if (sourceType.equals(SourceType.s_property)) {
			if (withAlias)
				return "a.properties->'" + value + "'";
			else
				return "properties->'" + value + "'";
		} else if (sourceType.equals(SourceType.s_field)) {
			if (withAlias) {
				return getFieldWithAlias((String) value, false);
			} else {
				return (String) value;
			}
		} else if (sourceType.equals(SourceType.s_const)) {
			if (value instanceof String) {
				Timestamp date = isValidDate((String) value);
				if (date != null)
					paramMap.put(uniqueName, date);
				else
					paramMap.put(uniqueName, value);
			} else
				paramMap.put(uniqueName, value);

			return ":" + uniqueName;
		}

		return null;
	}

	private String getConditionStringValue(SourceType sourceType, Object value) throws UnknownColumnException {
		if (sourceType.equals(SourceType.s_function)) {
			String sValue = (String) value;
			if (sValue.startsWith("$.") && sValue.endsWith(")")) {
				int lastDot = sValue.lastIndexOf('.');
				String fName = sValue.substring(0, lastDot);
				String functionName = sValue.substring(lastDot + 1);
				if (functionName.equals("length()"))
					return "(JSON_LENGTH(a.properties->'" + fName + "'))";
				else if (functionName.equals("isNotNull()"))
					return "(JSON_EXTRACT(a.properties, '" + fName + "') is not null)";
				else if (functionName.equals("isNull()"))
					return "(JSON_EXTRACT(a.properties, '" + fName + "') is null)";
				else if (functionName.equals("isNotEmpty()"))
					return "(JSON_EXTRACT(properties, '" + fName + "') is not null and JSON_LENGTH(properties, '"
							+ fName + "') > 0)";
				else if (functionName.equals("isEmpty()"))
					return "(JSON_EXTRACT(properties, '" + fName + "') is null and JSON_LENGTH(properties, '" + fName
							+ "') = 0)";
				else if (functionName.startsWith("contains(")) {
					String val = functionName.substring(functionName.indexOf("(") + 1, functionName.length() - 1);
					return "(JSON_SEARCH(properties->'" + fName + "', 'one', " + val + ") is not null)";
				} else if (functionName.startsWith("notContains(")) {
					String val = functionName.substring(functionName.indexOf("(") + 1, functionName.length() - 1);
					return "(JSON_SEARCH(properties->'" + fName + "', 'one', " + val + ") is null)";
				} else
					return sValue;
			} else
				return sValue;
		} else if (sourceType.equals(SourceType.s_property)) {
			if (withAlias)
				return "a.properties->'" + value + "'";
			else
				return "properties->'" + value + "'";
		} else if (sourceType.equals(SourceType.s_field)) {
			if (withAlias) {
				return getFieldWithAlias((String) value, false);
			} else {
				return (String) value;
			}
		} else if (sourceType.equals(SourceType.s_const)) {
			if (value instanceof String) {
				return "'" + value + "'";
			} else
				return (String) value;
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private String generateConditionString(String uniqueName, Condition condition, Map<String, Object> paramMap)
			throws UnknownColumnException {
		String sFormat = "%s %s %s";
		String v1 = null;
		String op = null;
		String v2 = null;

		if (condition.getOperator().equals(Operator.in) || condition.getOperator().equals(Operator.notin)) {
			if (condition.getRightSource().equals(SourceType.s_property)) {
				if (condition.getLeftValue() instanceof String)
					v1 = "JSON_SEARCH(a.properties->'" + condition.getRightValue() + "', 'one', '"
							+ condition.getLeftValue() + "')";
				else
					v1 = "JSON_SEARCH(a.properties->'" + condition.getRightValue() + "', 'one', "
							+ condition.getLeftValue() + ")";

				op = condition.getOperator().equals(Operator.in) ? Operator.isnot.toString() : Operator.is.toString();
				v2 = "null";
			} else if (condition.getLeftSource().equals(SourceType.s_property)
					&& condition.getRightValue() instanceof List) {
				List<String> items = (List<String>) condition.getRightValue();
				String result = "(";
				for (String item : items) {
					result += "JSON_SEARCH(a.properties->'" + condition.getLeftValue() + "', 'one', '" + item
							+ "') IS NOT NULL OR ";
				}
				result = result.substring(0, result.length() - 4); // cut last OR
				result += ")";
				return result;
			} else {
				sFormat = "%s %s (%s)";
				v1 = getConditionStringValue(uniqueName + "1", condition.getLeftSource(), condition.getLeftValue(),
						paramMap);
				op = condition.getOperator().toString();
				v2 = getConditionStringValue(uniqueName + "2", condition.getRightSource(), condition.getRightValue(),
						paramMap);
			}
		} else if (condition.getOperator().equals(Operator.like) || condition.getOperator().equals(Operator.notLike)) {
			// we have to add '"' character becouse MySQL returns strings with this
			// character on the first and last character
			// so if we have value text%, we have to verify "text%"
			String leftValue = (String) condition.getLeftValue();
			String rightValue = (String) condition.getRightValue();

			if (condition.getRightSource().equals(SourceType.s_property)
					&& condition.getLeftSource().equals(SourceType.s_const))
				leftValue = "\"" + condition.getLeftValue() + "\"";

			if (condition.getLeftSource().equals(SourceType.s_property)
					&& condition.getRightSource().equals(SourceType.s_const))
				rightValue = "\"" + condition.getRightValue() + "\"";

			v1 = getConditionStringValue(uniqueName + "1", condition.getLeftSource(), leftValue, paramMap);
			op = condition.getOperator().toString();
			v2 = getConditionStringValue(uniqueName + "2", condition.getRightSource(), rightValue, paramMap);
		} else {
			v1 = getConditionStringValue(uniqueName + "1", condition.getLeftSource(), condition.getLeftValue(),
					paramMap);
			op = condition.getOperator().toString();
			if (condition.getLeftSource().equals(SourceType.s_property) && condition.getRightValue() instanceof Boolean)
				v2 = Boolean.toString((boolean) condition.getRightValue());
			else
				v2 = getConditionStringValue(uniqueName + "2", condition.getRightSource(), condition.getRightValue(),
						paramMap);
		}

		return String.format(sFormat, v1, op, v2);
	}

	@SuppressWarnings("unchecked")
	private String generateConditionString(Condition condition) throws UnknownColumnException {
		String sFormat = "%s %s %s";
		String v1 = null;
		String op = null;
		String v2 = null;

		if (condition.getOperator().equals(Operator.in) || condition.getOperator().equals(Operator.notin)) {
			if (condition.getRightSource().equals(SourceType.s_property)) {
				if (condition.getLeftValue() instanceof String)
					v1 = "JSON_SEARCH(a.properties->'" + condition.getRightValue() + "', 'one', '"
							+ condition.getLeftValue() + "')";
				else
					v1 = "JSON_SEARCH(a.properties->'" + condition.getRightValue() + "', 'one', "
							+ condition.getLeftValue() + ")";

				op = condition.getOperator().equals(Operator.in) ? Operator.isnot.toString() : Operator.is.toString();
				v2 = "null";
			} else if (condition.getLeftSource().equals(SourceType.s_property)
					&& condition.getRightValue() instanceof List) {
				List<String> items = (List<String>) condition.getRightValue();
				String result = "(";
				for (String item : items) {
					result += "JSON_SEARCH(a.properties->'" + condition.getLeftValue() + "', 'one', '" + item
							+ "') IS NOT NULL OR ";
				}
				result = result.substring(0, result.length() - 4); // cut last OR
				result += ")";
				return result;
			} else {
				sFormat = "%s %s (%s)";
				v1 = getConditionStringValue(condition.getLeftSource(), condition.getLeftValue());
				op = condition.getOperator().toString();
				v2 = getConditionStringValue(condition.getRightSource(), condition.getRightValue());
			}
		} else if (condition.getOperator().equals(Operator.like) || condition.getOperator().equals(Operator.notLike)) {
			// we have to add '"' character becouse MySQL returns strings with this
			// character on the first and last character
			// so if we have value text%, we have to verify "text%"
			String leftValue = (String) condition.getLeftValue();
			String rightValue = (String) condition.getRightValue();

			if (condition.getRightSource().equals(SourceType.s_property)
					&& condition.getLeftSource().equals(SourceType.s_const))
				leftValue = "\"" + condition.getLeftValue() + "\"";

			if (condition.getLeftSource().equals(SourceType.s_property)
					&& condition.getRightSource().equals(SourceType.s_const))
				rightValue = "\"" + condition.getRightValue() + "\"";

			v1 = getConditionStringValue(condition.getLeftSource(), leftValue);
			op = condition.getOperator().toString();
			v2 = getConditionStringValue(condition.getRightSource(), rightValue);
		} else {
			v1 = getConditionStringValue(condition.getLeftSource(), condition.getLeftValue());
			op = condition.getOperator().toString();
			if (condition.getLeftSource().equals(SourceType.s_property) && condition.getRightValue() instanceof Boolean)
				v2 = Boolean.toString((boolean) condition.getRightValue());
			else
				v2 = getConditionStringValue(condition.getRightSource(), condition.getRightValue());
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
					String fName = fieldName.substring(0, lastDot);
					String functionName = fieldName.substring(lastDot + 1);
					if (functionName.equals("length()"))
						if (addAs)
							return "JSON_LENGTH(a.properties->'" + fName + "') as `" + fieldName + "`";
						else
							return "JSON_LENGTH(a.properties->'" + fName + "')";
					else if (functionName.equals("isNotNull()"))
						if (addAs)
							return "JSON_EXTRACT(a.properties, '" + fName + "') is not null as `" + fieldName + "`";
						else
							return "JSON_EXTRACT(a.properties, '" + fName + "')";
					else if (functionName.equals("isNull()"))
						if (addAs)
							return "JSON_EXTRACT(a.properties, '" + fName + "') is null as `" + fieldName + "`";
						else
							return "JSON_EXTRACT(a.properties, '" + fName + "')";
					else if (functionName.equals("isNotEmpty()"))
						if (addAs)
							return "(JSON_EXTRACT(properties, '" + fName + "') is not null and JSON_LENGTH(properties, '"
									+ fName + "') > 0) as `" + fieldName + "`";
						else
							return "(JSON_EXTRACT(properties, '" + fName + "') is not null and JSON_LENGTH(properties, '"
									+ fName + "') > 0)";
					else if (functionName.equals("isEmpty()"))
						if (addAs)
							return "(JSON_EXTRACT(properties, '" + fName + "') is null and JSON_LENGTH(properties, '"
									+ fName + "') = 0) as `" + fieldName + "`";
						else
							return "(JSON_EXTRACT(properties, '" + fName + "') is null and JSON_LENGTH(properties, '"
									+ fName + "') = 0)";
					else if (functionName.startsWith("contains(")) {
						String value = functionName.substring(functionName.indexOf("(") + 1, functionName.length() - 1);
						if (addAs)
							return "JSON_SEARCH(properties->'" + fName + "', 'one', " + value + ") is not null as `"
									+ fieldName + "`";
						else
							return "JSON_SEARCH(properties->'" + fName + "', 'one', " + value + ") is not null";
					} else if (functionName.startsWith("notContains(")) {
						String value = functionName.substring(functionName.indexOf("(") + 1, functionName.length() - 1);
						if (addAs)
							return "JSON_SEARCH(properties->'" + fName + "', 'one', " + value + ") is null as `" + fieldName
									+ "`";
						else
							return "JSON_SEARCH(properties->'" + fName + "', 'one', " + value + ") is null";
					} else
						throw new UnknownColumnException(fieldName);
				} else {
					if (addAs)
						return "a.properties->'" + fieldName + "' as '" + fieldName + "'";
					else
						return "a.properties->'" + fieldName + "'";
				}
			} else {
				switch (fieldName) {
				case COL.TAG_NAME:
					return "t." + fieldName;
				case COL.BUNDLE_ID:
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
							.replace(COL.BUNDLE_ID, "a." + COL.BUNDLE_ID)
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

	public Query orderBy(Optional<String> sort, String aliasChar) {
		if (sort.isPresent())
			return orderBy(sort.get(), aliasChar);
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

		if (column.toUpperCase().startsWith("RAND()"))
			orderBy("rand()", true);
		else
			orderBy(getFieldWithAlias(column, false), asc);

		return this;
	}

	public Query orderBy(String sort, String aliasChar) {
		boolean asc = true;
		String column = sort;

		if (column.startsWith("desc(")) {
			column = column.replace("desc(", "");
			column = column.substring(0, column.length() - 1);
			asc = false;
		} else if (column.startsWith("asc(")) {
			column = column.replace("asc(", "");
			column = column.substring(0, column.length() - 1);
		}

		if (column.toUpperCase().startsWith("RAND()")) {
			orderBy("rand()", true);
		} else {
			if (column.startsWith("$."))
				column = "properties->'" + column + "'";

			if (aliasChar != null)
				column = aliasChar + "." + column;

			orderBy(column, asc);
		}

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

	public Query offset(Map<String, Object> paramMap, int offset) {
		query += " OFFSET :offset";
		paramMap.put("offset", offset);
		return this;
	}

	public String toString(Logger logger) {
		logger.debug(query);
		return query;
	}

	private String intoValues(MapSqlParameterSource mapParameters) {
		String columns = "";
		String values = "";
		for (Map.Entry<String, Object> prop : mapParameters.getValues().entrySet()) {
			columns += ", `" + prop.getKey() + "`";
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
				result += ", " + getFieldWithAlias((String) col.get(C.FIELD), false) + " as '" + col.get(C.TITLE)+ "'";
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
					result += ", " + alias + "." + col.getKey() + " = '" + col.getValue() + "'";
				else
					result += ", " + alias + "." + col.getKey() + " = " + col.getValue();
			} else {
				if (col.getValue() instanceof String)
					result += ", " + col.getKey() + " = '" + col.getValue() + "'";
				else
					result += ", " + col.getKey() + " = " + col.getValue();
			}
		return result.substring(2);
	}

}
