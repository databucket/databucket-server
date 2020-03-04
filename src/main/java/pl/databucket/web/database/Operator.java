package pl.databucket.web.database;

public enum Operator {
	
	equal("="),
	grater(">"),
	graterEqual(">="),
	notin("NOT IN"),
	in("IN"),
	is("IS"),
	isnot("IS NOT"),
	less("<"), 
	lessEqual("<="),
	like("LIKE"), 
	notEqual("<>"), 
	notLike("NOT LIKE");

	private final String symbol;
	
	private Operator(String text) {
		this.symbol = text;
	}
	
	public String toString() {
		return symbol;
	}	
	
	public static Operator fromString(String text) {
        for (Operator operator : Operator.values()) {
            if (operator.symbol.equalsIgnoreCase(text.toUpperCase())) {
                return operator;
            }
        }
        return null;
    }
}
