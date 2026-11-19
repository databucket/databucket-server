package pl.databucket.server.service.data;

/** The operators of the conditions the server adds to a data filter (see LogicFilter.require). */
public enum Operator {
    equal,
    notEqual,
    in
}
