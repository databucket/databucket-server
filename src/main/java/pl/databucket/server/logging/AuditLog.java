package pl.databucket.server.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Log of significant operations: sign-ins, password and project changes, creating, modifying and deleting
 * entities and bucket data. The action (e.g. "bucket.create") is also a separate field in the JSON log.
 * Inside a transaction the entry is written only after the commit, so rolled back operations are not logged.
 * The arguments are evaluated when log() is called.
 */
public final class AuditLog {

    // One logger per area (the part of the action before the dot), e.g. pl.databucket.server.operation.data,
    // so a busy area can be turned down on its own. Not "audit": abbreviated in the text log it would read
    // p.d.s.a.data, the same as the access log of bucket data.
    private static final String LOGGER_PREFIX = "pl.databucket.server.operation.";

    private AuditLog() {
    }

    public static void log(String action, String message, Object... args) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    write(action, message, args);
                }
            });
        } else {
            write(action, message, args);
        }
    }

    private static void write(String action, String message, Object... args) {
        MDC.put(LogContext.ACTION, action);
        try {
            Logger logger = LoggerFactory.getLogger(LOGGER_PREFIX + action.substring(0, action.indexOf('.')));
            logger.info(action + ": " + message, args);
        } finally {
            MDC.remove(LogContext.ACTION);
        }
    }
}
