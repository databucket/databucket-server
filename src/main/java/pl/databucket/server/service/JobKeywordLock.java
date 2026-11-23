package pl.databucket.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One run of a keyword at a time in a project, also with several instances: a PostgreSQL session advisory lock held
 * on a connection of its own while the jobs run (they run in transactions of their own). Without PostgreSQL (the H2
 * of the tests) only within this instance.
 */
@Component
public class JobKeywordLock {

    private static final Logger logger = LoggerFactory.getLogger(JobKeywordLock.class);

    // the first key of the advisory lock: "DBJK" (the second one is the hash of the project and the keyword)
    private static final int KEY = 0x44424A4B;

    private final DataSource dataSource;
    private final Set<String> running = ConcurrentHashMap.newKeySet();

    public JobKeywordLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** The lock while it's held; closing it releases it. */
    public interface Held extends AutoCloseable {
        @Override
        void close();
    }

    /** @return the lock, or null when the keyword is already running in the project */
    public Held tryLock(int projectId, String keyword) {
        String id = projectId + ":" + keyword.toLowerCase(Locale.ROOT);
        if (!running.add(id))
            return null;
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            if (connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgres")) {
                if (!advisory(connection, "SELECT pg_try_advisory_lock(?, ?)", id)) {
                    connection.close();
                    running.remove(id);
                    return null;
                }
            } else {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            close(connection);
            running.remove(id);
            throw new IllegalStateException("The lock of the keyword could not be taken: " + e.getMessage(), e);
        }
        Connection held = connection;
        return () -> {
            try {
                if (held != null)
                    advisory(held, "SELECT pg_advisory_unlock(?, ?)", id);
            } catch (SQLException e) {
                logger.warn("The lock of the keyword {} could not be released: {}", id, e.getMessage());
            } finally {
                close(held);
                running.remove(id);
            }
        };
    }

    private static boolean advisory(Connection connection, String sql, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, KEY);
            statement.setInt(2, id.hashCode());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private static void close(Connection connection) {
        if (connection != null)
            try {
                connection.close();
            } catch (SQLException e) {
                logger.warn("Closing the connection of the keyword lock: {}", e.getMessage());
            }
    }
}
