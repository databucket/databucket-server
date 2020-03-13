package pl.databucket.database;

public enum SourceType {
	
	s_const("const"),
	s_field("field"), 
	s_property("property"), 
	s_function("function");

	private final String sourceName;
	
	private SourceType(String sourceName) {
		this.sourceName = sourceName;
	}
	
	public String toString() {
		return sourceName;
	}	
	
	public static SourceType fromString(String sourceName) {
        for (SourceType sType : SourceType.values()) {
            if (sType.sourceName.equalsIgnoreCase(sourceName)) {
                return sType;
            }
        }
        return null;
    }
}
