package pl.databucket.server.service.data.filter;

import pl.databucket.server.dto.IndexedPropertyDto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * The SQL of an index on a property of the data rows ({@link IndexedPropertyDto}). The index expressions are the
 * same as those {@link LogicSql} generates for a rule on the property, so PostgreSQL uses the index for the rule:
 * <ul>
 * <li>text: {@code (properties #>> ARRAY['a', 'b'])} with text_pattern_ops (==, in, like 'A%');</li>
 * <li>number: {@code (CASE WHEN jsonb_typeof((properties #> ARRAY['a', 'b'])) = 'number'
 * THEN ((properties #> ARRAY['a', 'b']))::numeric END)} (==, &lt;, &gt;, between).</li>
 * </ul>
 * The path is written into the SQL as a literal (also by LogicSql for an indexed path, so that the index works in
 * generic plans of prepared statements too); it comes only from the settings of the bucket and is checked here.
 */
public final class PropertyIndex {

    private PropertyIndex() {
    }

    /** The path as a text array literal, e.g. ARRAY['a', 'b']. */
    public static String pathLiteral(String path) {
        check(path);
        List<String> nodes = new ArrayList<>();
        for (String node : path.substring(2).split("\\."))
            nodes.add("'" + node.replace("'", "''") + "'");
        return "ARRAY[" + String.join(", ", nodes) + "]";
    }

    /** The property as text, as LogicSql compares it with a text. */
    public static String textExpression(String pathSql) {
        return "(properties #>> " + pathSql + ")";
    }

    /** The property as jsonb. */
    public static String jsonExpression(String pathSql) {
        return "(properties #> " + pathSql + ")";
    }

    /** The property as a number (null when it isn't one), as LogicSql compares it with a number. */
    public static String numberExpression(String pathSql) {
        String json = jsonExpression(pathSql);
        return "(CASE WHEN jsonb_typeof(" + json + ") = 'number' THEN (" + json + ")::numeric END)";
    }

    /** The name of the index of the property on the table: "x-bucket-N-p-" and a hash of the type and the path. */
    public static String indexName(String table, IndexedPropertyDto property) {
        return namePrefix(table) + hash(property.getType() + ":" + property.getPath());
    }

    /** The beginning of the names of all property indexes of the table. */
    public static String namePrefix(String table) {
        return table + "-p-";
    }

    /** CREATE INDEX of the property on the table. */
    public static String createSql(String table, IndexedPropertyDto property, boolean concurrently) {
        String path = pathLiteral(property.getPath());
        String expression = IndexedPropertyDto.NUMBER.equals(property.getType())
                ? numberExpression(path)
                : textExpression(path) + " text_pattern_ops";
        return "CREATE INDEX " + (concurrently ? "CONCURRENTLY " : "") + "IF NOT EXISTS \"" + indexName(table, property)
                + "\" ON \"" + table + "\" (" + expression + ")";
    }

    private static void check(String path) {
        if (path == null || !path.matches(IndexedPropertyDto.PATH_PATTERN))
            throw new IllegalArgumentException("Invalid indexed property path: " + path);
    }

    private static String hash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
