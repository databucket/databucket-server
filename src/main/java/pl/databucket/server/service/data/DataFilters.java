package pl.databucket.server.service.data;

import pl.databucket.server.exception.InvalidRuleException;
import pl.databucket.server.service.data.v2.LogicFilter;

import java.io.InvalidObjectException;

/**
 * Chooses the filter of a data request by the header Databucket-Api-Version: none or 1 is API version 1 (rules,
 * conditions, logic, also SQL text); 2 is version 2 (rules and logic, translated safely; no SQL text, no conditions).
 */
public final class DataFilters {

    public static final String VERSION_HEADER = "Databucket-Api-Version";

    private DataFilters() {
    }

    public static DataFilter of(String currentUser, SearchRules searchRules, String apiVersion)
            throws InvalidRuleException, InvalidObjectException {
        String version = apiVersion == null ? "1" : apiVersion.trim();
        return switch (version) {
            case "", "1" -> new QueryRule(currentUser, searchRules);
            case "2" -> LogicFilter.of(currentUser, searchRules);
            default -> throw new InvalidRuleException("Unknown " + VERSION_HEADER + ": " + apiVersion + " (use 1 or 2)");
        };
    }
}
