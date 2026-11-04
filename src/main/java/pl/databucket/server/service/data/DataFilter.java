package pl.databucket.server.service.data;

import pl.databucket.server.exception.ConditionNotAllowedException;
import pl.databucket.server.exception.UnknownColumnException;

import java.util.Map;

/**
 * The filter of a data request: which rows of a bucket table an operation reads, reserves, changes or removes.
 * {@link QueryRule} is API version 1, {@link pl.databucket.server.service.data.v2.LogicFilter} version 2
 * (header Databucket-Api-Version: 2); see {@link DataFilters}.
 */
public interface DataFilter {

    /** A condition the server adds on a column of the table (e.g. the rows of the user), combined with AND. */
    void require(String column, Operator operator, Object value);

    /** Adds the WHERE clause to the query; its values go into the parameters. */
    Query applyTo(Query query, Map<String, Object> paramMap) throws UnknownColumnException, ConditionNotAllowedException;
}
