package pl.databucket.server.service.data;

public enum Operator {

	not("!"),
	and("and"),
	or("or"),

	equal("="),
	notEqual("<>"),
	greater(">"),
	greaterEqual(">="),
	less("<"),
	lessEqual("<="),

	isNotEmpty("!!"),

	in("in"),
	notIn("!in"),

	is("is"),
	isNot("is not"),

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
		switch (symbol) {
			case "!like":
				return "not like";
			case "similar":
				return "similar to";
			case "!similar":
				return "not similar to";
			default:
				return symbol;
		}
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
			case greater: return less;
			case less: return greater;
			case greaterEqual: return lessEqual;
			case lessEqual: return greaterEqual;
			default: return null;
		}
	}
}
