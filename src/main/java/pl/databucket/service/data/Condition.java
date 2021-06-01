package pl.databucket.service.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Condition {
	
	private final SourceType leftSource;
	private final Object leftValue;
	private final Operator operator;
	private final SourceType rightSource;
	private Object rightValue;
	
	public Condition(SourceType leftSource, Object leftValue, Operator operator, SourceType rightSource, Object rightValue) {
		this.leftSource = leftSource;
		this.leftValue = leftValue;
		this.operator = operator;
		this.rightSource = rightSource;
		this.rightValue = rightValue;
	}
	
	public Condition(String field, Operator operator, Object value) {
		this.leftSource = field.startsWith("$") ? field.endsWith("()") ? SourceType.s_function : SourceType.s_property : SourceType.s_field;
		this.leftValue = field;
		this.operator = operator;
		this.rightSource = SourceType.s_const;
		this.rightValue = value;
	}

	private Object convertToColumnName(Object value) {
		if (value instanceof String)
			switch((String) value) {
				case "id": return "data_id";
				case "tagId": return "tag_id";
				case "reserved": return "reserved";
				case "owner": return "reserved_by";
				case "properties": return "properties";
				case "createdBy": return "created_by";
				case "createdAt": return "created_at";
				case "modifiedBy": return "modified_by";
				case "modifiedAt": return "modified_at";
				default: return value;
			}
		else
			return value;
	}
	
	public Condition(Map<String, Object> conditionMap) {
		this.leftSource = SourceType.fromString((String) conditionMap.get(Constants.LEFT_SOURCE));
		this.operator = Operator.fromString((String) conditionMap.get(Constants.OPERATOR));
		this.rightSource = SourceType.fromString((String) conditionMap.get(Constants.RIGHT_SOURCE));
		
		this.leftValue = convertToColumnName(conditionMap.get(Constants.LEFT_VALUE));
		this.rightValue = convertToColumnName(conditionMap.get(Constants.RIGHT_VALUE));
				
		if ((operator.equals(Operator.in) || operator.equals(Operator.notIn)) && rightSource.equals(SourceType.s_const) && !(rightValue instanceof List)) {
			String rValue = (String) rightValue;
			if (rValue.contains(",")) {
				
				// cut [] brackets
				if (rValue.startsWith("["))
					rValue = rValue.substring(1);
				
				if (rValue.endsWith("]"))
					rValue = rValue.substring(0, rValue.length() -1 );
				
				String[] valuesStr = (rValue).split(",");
				
				// clean spaces and quotation marks
				for (int i = 0; i < valuesStr.length; i++) {
					valuesStr[i] = valuesStr[i].trim();
					if (valuesStr[i].startsWith("\""))
						valuesStr[i] = valuesStr[i].substring(1);
					if (valuesStr[i].endsWith("\""))
						valuesStr[i] = valuesStr[i].substring(0, valuesStr[i].length() - 1);
				}
				
				// check if values are numbers
				boolean intValues;
				boolean floatValues = false;
				
				try {
					intValues = true;
					for (String vStr : valuesStr)
						Integer.parseInt(vStr);
				} catch (NumberFormatException e) {
					intValues = false;
				}
				
				if (!intValues) {
					floatValues = true;
					try {
						for (String vStr : valuesStr)
							Float.parseFloat(vStr);
					} catch (NumberFormatException e) {
						floatValues = false;
					}
				}
				
				if (intValues) {
					List<Integer> listOfValues = new ArrayList<>();
					for (String vStr : valuesStr)
						listOfValues.add(Integer.parseInt(vStr));
					this.rightValue = listOfValues;
				} else if (floatValues) {
					List<Float> listOfValues = new ArrayList<>();
					for (String vStr : valuesStr)
						listOfValues.add(Float.parseFloat(vStr));
					this.rightValue = listOfValues;
				} else {
					List<String> listOfValues = new ArrayList<>();
					Collections.addAll(listOfValues, valuesStr);
					this.rightValue = listOfValues;
				}				
			}
		}
	}

	public SourceType getLeftSource() {
		return leftSource;
	}

	public Object getLeftValue() {
		return leftValue;
	}

	public Operator getOperator() {
		return operator;
	}

	public SourceType getRightSource() {
		return rightSource;
	}

	public Object getRightValue() {
		return rightValue;
	}

	public String toString() {
		String result = "";
		result += " leftSource: " + getLeftSource();
		result += " | leftValue: " + getLeftValue();
		result += " | operator: " + getOperator();
		result += " | rightSource: " + getRightSource();
		result += " | rightValue: " + getRightValue();

		return result;
	}
}
