// The JsonLogic of the query builder from the rules of the API ("rules", see the wiki page Rules): the reverse of
// LogicToRules.js, so that rules saved as a list (Settings > Jobs) can be edited in the builder.
//
//   [rule, ...] (combined with "and")                   -> {"and": [...]}
//   {"and": [...]}, {"or": [...]}, {"!": {...} or [...]} -> {"and": [...]}, {"or": [...]}, {"!": {...}}
//   ["$.a.b", "==", 1] (also = != <> < <= > >=)          -> {"==": [{"var": "prop.$*a*b"}, 1]}
//   [field, "in", [1, 2]]                                -> {"in": [{"var": field}, [1, 2]]}
//   [field, "like", "%text%"] (no other wildcards)       -> {"in": ["text", {"var": field}]} (contains)
//   ["$.a.isEmpty()", "==", true] / [field, "==", null]  -> {"!": {"var": ...}} (is empty; != / false: {"!!": ...})
// Returns null for anything else (other operators, two variables in a rule, a text rule...): such rules stay JSON.

import {FIELDS} from './LogicToRules';

const OPERATORS = {'=': '==', '==': '==', '<>': '!=', '!=': '!=', '<': '<', '<=': '<=', '>': '>', '>=': '>='};

class Unsupported extends Error {
}

const isVariable = value => typeof value === 'string' && (value.startsWith('$') || FIELDS.includes(value));

// "$.address.city" -> "prop.$*address*city"; the fields of the data stay as they are
const toVar = field => {
    if (FIELDS.includes(field))
        return {var: field};
    if (!/^\$\.[^*]+$/.test(field) || field.endsWith('()'))
        throw new Unsupported();
    return {var: 'prop.' + field.replaceAll('.', '*')};
};

const constant = value => {
    if (isVariable(value))
        throw new Unsupported();
    return value;
};

// "%text%" with no wildcards inside (escaped ones allowed) -> "text"
const containsText = pattern => {
    if (typeof pattern !== 'string' || pattern.length < 2 || !pattern.startsWith('%') || !pattern.endsWith('%'))
        throw new Unsupported();
    const inner = pattern.substring(1, pattern.length - 1);
    let text = '';
    for (let i = 0; i < inner.length; i++) {
        const c = inner[i];
        if (c === '\\' && i + 1 < inner.length && '\\%_'.includes(inner[i + 1]))
            text += inner[++i];
        else if (c === '%' || c === '_' || c === '\\')
            throw new Unsupported();
        else
            text += c;
    }
    if (text === '')
        throw new Unsupported();
    return text;
};

const rule = ([left, operatorText, right]) => {
    if (typeof left !== 'string' || typeof operatorText !== 'string')
        throw new Unsupported();
    const operator = operatorText.trim().toLowerCase();

    // is empty / is not empty
    if (left.startsWith('$.') && left.endsWith('.isEmpty()') && typeof right === 'boolean'
        && (OPERATORS[operator] === '==' || OPERATORS[operator] === '!=')) {
        const empty = (OPERATORS[operator] === '==') === right;
        return {[empty ? '!' : '!!']: toVar(left.substring(0, left.length - '.isEmpty()'.length))};
    }
    if (right === null && FIELDS.includes(left) && (OPERATORS[operator] === '==' || OPERATORS[operator] === '!='))
        return {[OPERATORS[operator] === '==' ? '!' : '!!']: toVar(left)};

    const variable = toVar(left);
    if (OPERATORS[operator] != null && right !== null && !Array.isArray(right) && typeof right !== 'object')
        return {[OPERATORS[operator]]: [variable, constant(right)]};
    if (operator === 'in' && Array.isArray(right) && right.length > 0
        && right.every(item => item !== null && typeof item !== 'object'))
        return {in: [variable, right]};
    if (operator === 'like')
        return {in: [containsText(right), variable]};
    throw new Unsupported();
};

const item = value => {
    if (Array.isArray(value)) {
        if (value.length !== 3 || typeof value[1] !== 'string' || Array.isArray(value[0]))
            throw new Unsupported();
        return rule(value);
    }
    if (value == null || typeof value !== 'object' || Object.keys(value).length !== 1)
        throw new Unsupported();
    const [operator, args] = Object.entries(value)[0];
    if (operator === 'and' || operator === 'or') {
        if (!Array.isArray(args) || args.length === 0)
            throw new Unsupported();
        return {[operator]: args.map(item)};
    }
    if (operator === '!') {
        // {"!": [rule, ...]} = not all of them; {"!": rule or group}
        const listOfItems = Array.isArray(args) && args.length > 0 && (Array.isArray(args[0]) || typeof args[0] === 'object');
        return {'!': listOfItems ? {and: args.map(item)} : item(args)};
    }
    throw new Unsupported();
};

/**
 * The JsonLogic of a list of rules: {} without rules, null when the builder can't show them.
 */
export const rulesToLogic = (rules) => {
    if (rules == null || (Array.isArray(rules) && rules.length === 0))
        return {};
    if (!Array.isArray(rules))
        return null;
    try {
        return {and: rules.map(item)};
    } catch (e) {
        if (e instanceof Unsupported)
            return null;
        throw e;
    }
};

// groups of one item as that item, dates as ISO text: two logics that mean the same compare equal
const normalize = logic => {
    if (logic instanceof Date)
        return logic.toISOString();
    if (Array.isArray(logic))
        return logic.map(normalize);
    if (logic != null && typeof logic === 'object') {
        const entries = Object.entries(logic);
        if (entries.length === 1 && (entries[0][0] === 'and' || entries[0][0] === 'or')
            && Array.isArray(entries[0][1]) && entries[0][1].length === 1)
            return normalize(entries[0][1][0]);
        return Object.fromEntries(entries.map(([key, value]) => [key, normalize(value)]));
    }
    return logic;
};

/**
 * Whether two JsonLogic expressions are the same apart from groups of one item (the builder may add or drop them).
 */
export const sameLogic = (a, b) => JSON.stringify(normalize(a ?? {})) === JSON.stringify(normalize(b ?? {}));
