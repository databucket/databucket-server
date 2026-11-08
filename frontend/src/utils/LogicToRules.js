// The rules of the API ("rules", see the wiki page Rules) from the JsonLogic the rich filter builds ("logic"),
// so that the filter can be copied into a request of an API client (Postman, Bruno, a test...).
//
// Handles what the query builder of the rich filter produces:
//   {"and": [...]}, {"or": [...]}, {"!": {...}}         -> {"and": [...]}, {"or": [...]}, {"!": {...}}
//   {"==": [{"var": "prop.$*a*b"}, 1]} (also != < <= > >=) -> ["$.a.b", "==", 1]
//   {"<=": [1, {"var": "id"}, 9]} (between)            -> ["id", ">=", 1], ["id", "<=", 9]
//   {"in": [{"var": ...}, [1, 2]]} (any in)              -> [field, "in", [1, 2]]
//   {"in": ["text", {"var": ...}]} (contains)            -> [field, "like", "%text%"]
//   {"!": {"var": ...}} / {"!!": {"var": ...}} (is empty / is not empty)
//                                                        -> ["$.a.isEmpty()", "==", true] or [field, "==", null]
// Returns null when the logic has something else: then the rules can't be shown.

const COMPARISONS = ['==', '!=', '<', '<=', '>', '>='];
const INVERTED = {'<': '>', '<=': '>='};

const isVar = item => item != null && typeof item === 'object' && !Array.isArray(item)
    && Object.keys(item).length === 1 && typeof item.var === 'string';

// "prop.$*address*city" -> "$.address.city"; the fields of the data (id, tagId, owner, ...) stay as they are
const toField = variable => variable.var.startsWith('prop.$*')
    ? variable.var.substring('prop.'.length).replaceAll('*', '.')
    : variable.var;

const isProperty = field => field.startsWith('$.');

class Unsupported extends Error {
}

// one item of a list of rules (a rule or a group), or several rules for a "between"
const convert = (logic) => {
    if (logic == null || typeof logic !== 'object' || Array.isArray(logic) || Object.keys(logic).length !== 1)
        throw new Unsupported();
    const [operator, args] = Object.entries(logic)[0];

    if (operator === 'and' || operator === 'or') {
        if (!Array.isArray(args))
            throw new Unsupported();
        return [{[operator]: args.flatMap(convert)}];
    }

    if (operator === '!' || operator === '!!') {
        const value = Array.isArray(args) && args.length === 1 ? args[0] : args;
        if (isVar(value)) {
            const field = toField(value);
            const empty = operator === '!';
            return [isProperty(field)
                ? [`${field}.isEmpty()`, '==', empty]
                : [field, empty ? '==' : '!=', null]];
        }
        if (operator === '!!')
            throw new Unsupported();
        // a group ({"or": ...}) as it is, rules as a list of rules (a single rule alone would be read as text rules)
        const negated = convert(value);
        return [{'!': negated.length === 1 && !Array.isArray(negated[0]) ? negated[0] : negated}];
    }

    if (!Array.isArray(args))
        throw new Unsupported();

    if (COMPARISONS.includes(operator)) {
        if (args.length === 2 && isVar(args[0]) && !isVar(args[1]))
            return [[toField(args[0]), operator, args[1]]];
        if (args.length === 3 && INVERTED[operator] && !isVar(args[0]) && isVar(args[1]) && !isVar(args[2])) {
            const field = toField(args[1]);
            return [[field, INVERTED[operator], args[0]], [field, operator, args[2]]];
        }
        throw new Unsupported();
    }

    if (operator === 'in' && args.length === 2) {
        if (isVar(args[0]) && Array.isArray(args[1]))
            return [[toField(args[0]), 'in', args[1]]];
        if (typeof args[0] === 'string' && isVar(args[1]))
            return [[toField(args[1]), 'like', `%${args[0]}%`]];
    }
    throw new Unsupported();
};

/**
 * The "rules" of a request for the logic of the rich filter: [] without logic, null when it can't be converted.
 */
export const logicToRules = (logic) => {
    if (logic == null || Object.keys(logic).length === 0)
        return [];
    try {
        // the rules of a list are combined with "and": a top-level "and" needs no group
        if (logic.and != null && Object.keys(logic).length === 1 && Array.isArray(logic.and))
            return logic.and.flatMap(convert);
        return convert(logic);
    } catch (e) {
        if (e instanceof Unsupported)
            return null;
        throw e;
    }
};
