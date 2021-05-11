package pl.databucket.service.data;

public enum Operator {
	
	equal("="),
	grater(">"),
	graterEqual(">="),
	less("<"),
	lessEqual("<="),
	notEqual("<>"),

	in("IN"),
	notIn("NOT IN"),

	like("LIKE"),
	notLike("NOT LIKE"),

	similarTo("SIMILAR TO"),
	notSimilarTo("NOT SIMILAR TO"),

	matchesCS("~"),
	notMatchCS("!~"),

	matchesCI("~*"),
	notMatchCI("!~*");

	private final String symbol;
	
	Operator(String text) {
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
