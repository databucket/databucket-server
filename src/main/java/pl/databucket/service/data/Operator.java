package pl.databucket.service.data;

public enum Operator {

	not("!"),
	and("and"),
	or("or"),

	equal("="),
	notEqual("<>"),
	grater(">"),
	graterEqual(">="),
	less("<"),
	lessEqual("<="),

	isNotEmpty("!!"),

	in("in"),
	notIn("!in"),

	like("like"),
	notLike("!like"),

	similarTo("similar"),
	notSimilarTo("!similar"),

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
            if (operator.symbol.equalsIgnoreCase(text.toLowerCase().replace("==", "=").replace("!=", "<>"))) {
                return operator;
            }
        }
        return null;
    }

    public static Operator getInverted(Operator operator) {
		switch (operator) {
			case grater: return less;
			case less: return grater;
			case graterEqual: return lessEqual;
			case lessEqual: return graterEqual;
			default: return null;
		}
	}
}
