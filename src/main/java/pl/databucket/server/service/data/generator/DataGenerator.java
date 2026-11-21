package pl.databucket.server.service.data.generator;

import net.datafaker.Faker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates the properties of data rows from a spec: a generator per property path. The spec is checked when the
 * generator is made; an error says which field is wrong and how to write it. A generator made with a seed gives the
 * same rows again (for the same spec, locale and numbers).
 *
 * <ul>
 *   <li>a text with {@code #{...}}: a Datafaker expression, e.g. {@code "#{Name.firstName}"}, {@code "#{Address.city}"},
 *   {@code "#{numerify '###-###'}"}; {@code {n}} in a text is the number of the row, e.g. {@code "user-{n}@example.com"}
 *   (unique)</li>
 *   <li>any other text, number, boolean or null: that value in every row</li>
 *   <li>{@code {"number": [min, max], "decimals": 2}}: a random number in the range (decimals default 0)</li>
 *   <li>{@code {"oneOf": [values], "weights": [numbers]}}: one of the values, optionally weighted</li>
 *   <li>{@code {"date": ["2024-01-01", "2024-12-31"]}}: a date (yyyy-MM-dd) in the range</li>
 *   <li>{@code {"datetime": ["2024-01-01", "2024-12-31T23:59:59Z"]}}: a UTC timestamp (ISO 8601) in the range</li>
 * </ul>
 */
public final class DataGenerator {

    public static final int MAX_FIELDS = 100;
    static final String PROVIDERS_URL = "https://www.datafaker.net/documentation/providers/";
    static final int MAX_DECIMALS = 10;
    static final BigDecimal MAX_NUMBER = new BigDecimal("1e15");
    private static final Set<String> KINDS = Set.of("number", "oneOf", "date", "datetime");
    private static final String EXAMPLES = "Write a field as a text with a Datafaker expression (\"#{Name.firstName}\", "
            + "\"#{Internet.emailAddress}\", \"#{numerify '###-###'}\"), a text with {n} for the row number "
            + "(\"user-{n}\"), a constant, {\"number\": [min, max], \"decimals\": 2}, {\"oneOf\": [values], "
            + "\"weights\": [numbers]}, {\"date\": [from, to]} or {\"datetime\": [from, to]}";

    @FunctionalInterface
    private interface Generator {
        Object next(long n);
    }

    private record Field(String name, List<String> path, Generator generator) {
    }

    private final Random random;
    private final Faker faker;
    private final List<Field> fields = new ArrayList<>();

    private DataGenerator(Locale locale, Long seed) {
        this.random = seed != null ? new Random(seed) : new Random();
        this.faker = new Faker(locale, random);
    }

    /**
     * @param spec   property path ($.a.b or a.b) -> generator
     * @param locale language of the Datafaker values, e.g. en, pl, de-DE; null = en
     * @param seed   null = different rows every time
     */
    public static DataGenerator of(Map<String, Object> spec, String locale, Long seed) {
        if (spec == null || spec.isEmpty())
            throw new IllegalArgumentException("Give at least one field. " + EXAMPLES);
        if (spec.size() > MAX_FIELDS)
            throw new IllegalArgumentException("At most " + MAX_FIELDS + " fields");
        DataGenerator generator = new DataGenerator(locale(locale), seed);
        List<List<String>> paths = new ArrayList<>();
        spec.forEach((name, value) -> {
            List<String> path = path(name);
            for (List<String> other : paths)
                if (startsWith(path, other) || startsWith(other, path))
                    throw new IllegalArgumentException("The fields '" + name + "' and '$." + String.join(".", other)
                            + "' overlap: one is inside the other");
            paths.add(path);
            generator.fields.add(new Field(name, path, generator.generator(name, value)));
        });
        return generator;
    }

    /** The properties of the row with number n. */
    public Map<String, Object> next(long n) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (Field field : fields) {
            Object value;
            try {
                value = field.generator().next(n);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Field '" + field.name() + "': " + e.getMessage() + ". " + EXAMPLES);
            }
            put(row, field.path(), value);
        }
        return row;
    }

    public List<Map<String, Object>> next(long firstN, int count) {
        List<Map<String, Object>> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            rows.add(next(firstN + i));
        return rows;
    }

    // ---------------------------------------------------------------- spec

    private Generator generator(String name, Object value) {
        if (value instanceof String text)
            return text(name, text);
        if (value == null || value instanceof Number || value instanceof Boolean)
            return n -> value;
        if (value instanceof Map<?, ?> map)
            return object(name, map);
        throw new IllegalArgumentException("Field '" + name + "': a list isn't a generator. " + EXAMPLES);
    }

    private Generator text(String name, String text) {
        boolean number = text.contains("{n}");
        boolean expression = text.contains("#{");
        if (!number && !expression)
            return n -> text;
        if (expression && text.lastIndexOf('}') < text.lastIndexOf("#{"))
            throw new IllegalArgumentException("Field '" + name + "': the expression " + text.substring(text.lastIndexOf("#{"))
                    + " has no closing }, e.g. #{Name.firstName}");
        return n -> {
            String value = number ? text.replace("{n}", Long.toString(n)) : text;
            if (!expression)
                return value;
            try {
                return faker.expression(value);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(unknownExpression(name, value, e));
            }
        };
    }

    private static final Pattern UNRESOLVED = Pattern.compile("Unable to resolve (#\\{.*?}) directive");

    // Datafaker's message names its internal context; say which expression it doesn't know and how to write one
    private static String unknownExpression(String name, String text, RuntimeException e) {
        Matcher unresolved = UNRESOLVED.matcher(String.valueOf(e.getMessage()));
        String expression = unresolved.find() ? unresolved.group(1) : text;
        return "Field '" + name + "': Datafaker doesn't know " + expression + ". Write a provider and its method, "
                + "e.g. #{Name.firstName}, #{Address.city}, #{Internet.emailAddress}, #{PhoneNumber.cellPhone}, "
                + "or a function with its argument, e.g. #{numerify '###-###'}, #{letterify '???'}; "
                + "all providers: " + PROVIDERS_URL;
    }

    private Generator object(String name, Map<?, ?> map) {
        List<String> kinds = map.keySet().stream().map(String::valueOf).filter(KINDS::contains).toList();
        if (kinds.size() != 1)
            throw new IllegalArgumentException("Field '" + name + "': give exactly one of number, oneOf, date, datetime. "
                    + EXAMPLES);
        String kind = kinds.get(0);
        Set<String> allowed = switch (kind) {
            case "number" -> Set.of("number", "decimals");
            case "oneOf" -> Set.of("oneOf", "weights");
            default -> Set.of(kind);
        };
        for (Object key : map.keySet())
            if (!allowed.contains(String.valueOf(key)))
                throw new IllegalArgumentException("Field '" + name + "': '" + key + "' doesn't go with '" + kind
                        + "'; allowed: " + String.join(", ", allowed));
        return switch (kind) {
            case "number" -> number(name, map.get("number"), map.get("decimals"));
            case "oneOf" -> oneOf(name, map.get("oneOf"), map.get("weights"));
            case "date" -> date(name, map.get("date"));
            default -> datetime(name, map.get("datetime"));
        };
    }

    private Generator number(String name, Object range, Object decimalsValue) {
        List<?> bounds = range(name, "number", range);
        BigDecimal min = decimal(name, bounds.get(0));
        BigDecimal max = decimal(name, bounds.get(1));
        if (min.compareTo(max) > 0)
            throw new IllegalArgumentException("Field '" + name + "': min is greater than max");
        if (min.abs().compareTo(MAX_NUMBER) > 0 || max.abs().compareTo(MAX_NUMBER) > 0)
            throw new IllegalArgumentException("Field '" + name + "': numbers up to " + MAX_NUMBER.toPlainString());
        int decimals = 0;
        if (decimalsValue != null) {
            if (!(decimalsValue instanceof Number d) || d.doubleValue() != d.intValue()
                    || d.intValue() < 0 || d.intValue() > MAX_DECIMALS)
                throw new IllegalArgumentException("Field '" + name + "': decimals is 0.." + MAX_DECIMALS);
            decimals = d.intValue();
        }
        if (decimals == 0) {
            long from = min.setScale(0, RoundingMode.CEILING).longValueExact();
            long to = max.setScale(0, RoundingMode.FLOOR).longValueExact();
            if (from > to)
                throw new IllegalArgumentException("Field '" + name + "': no whole number in the range; give decimals");
            return n -> random.nextLong(from, to + 1);
        }
        int scale = decimals;
        BigDecimal span = max.subtract(min);
        return n -> min.add(span.multiply(BigDecimal.valueOf(random.nextDouble()))).setScale(scale, RoundingMode.HALF_UP);
    }

    private Generator oneOf(String name, Object valuesObject, Object weightsObject) {
        if (!(valuesObject instanceof List<?> values) || values.isEmpty())
            throw new IllegalArgumentException("Field '" + name + "': oneOf is a non-empty list of values");
        List<Object> choices = new ArrayList<>(values);
        if (weightsObject == null)
            return n -> choices.get(random.nextInt(choices.size()));
        if (!(weightsObject instanceof List<?> weights) || weights.size() != choices.size())
            throw new IllegalArgumentException("Field '" + name + "': weights is a list of numbers, one per value of oneOf");
        double[] cumulative = new double[weights.size()];
        double sum = 0;
        for (int i = 0; i < weights.size(); i++) {
            if (!(weights.get(i) instanceof Number w) || w.doubleValue() < 0)
                throw new IllegalArgumentException("Field '" + name + "': weights are numbers >= 0");
            sum += w.doubleValue();
            cumulative[i] = sum;
        }
        if (sum <= 0)
            throw new IllegalArgumentException("Field '" + name + "': at least one weight must be above 0");
        double total = sum;
        return n -> {
            double x = random.nextDouble() * total;
            for (int i = 0; i < cumulative.length; i++)
                if (x < cumulative[i])
                    return choices.get(i);
            return choices.get(choices.size() - 1);
        };
    }

    private Generator date(String name, Object range) {
        List<?> bounds = range(name, "date", range);
        LocalDate from = localDate(name, bounds.get(0));
        LocalDate to = localDate(name, bounds.get(1));
        if (from.isAfter(to))
            throw new IllegalArgumentException("Field '" + name + "': the first date is after the second");
        long days = ChronoUnit.DAYS.between(from, to);
        return n -> from.plusDays(random.nextLong(0, days + 1)).toString();
    }

    private Generator datetime(String name, Object range) {
        List<?> bounds = range(name, "datetime", range);
        Instant from = instant(name, bounds.get(0));
        Instant to = instant(name, bounds.get(1));
        if (from.isAfter(to))
            throw new IllegalArgumentException("Field '" + name + "': the first time is after the second");
        long seconds = ChronoUnit.SECONDS.between(from, to);
        return n -> from.plusSeconds(random.nextLong(0, seconds + 1)).toString();
    }

    // ---------------------------------------------------------------- helpers

    private static List<?> range(String name, String kind, Object range) {
        if (!(range instanceof List<?> bounds) || bounds.size() != 2)
            throw new IllegalArgumentException("Field '" + name + "': " + kind + " is a list [from, to]");
        return bounds;
    }

    private static BigDecimal decimal(String name, Object value) {
        if (!(value instanceof Number number))
            throw new IllegalArgumentException("Field '" + name + "': the range of number has two numbers");
        return new BigDecimal(number.toString());
    }

    private static LocalDate localDate(String name, Object value) {
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Field '" + name + "': dates are yyyy-MM-dd, not '" + value + "'");
        }
    }

    private static Instant instant(String name, Object value) {
        String text = String.valueOf(value);
        try {
            if (text.length() == 10)
                return LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC);
            if (text.endsWith("Z") || text.matches(".*[+-]\\d\\d:\\d\\d$"))
                return OffsetDateTime.parse(text).toInstant();
            return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Field '" + name + "': times are yyyy-MM-dd or ISO 8601 such as "
                    + "2024-12-31T23:59:59Z, not '" + value + "'");
        }
    }

    private static Locale locale(String locale) {
        if (locale == null || locale.isBlank())
            return Locale.ENGLISH;
        Locale result = Locale.forLanguageTag(locale.replace('_', '-'));
        if (result.getLanguage().isEmpty())
            throw new IllegalArgumentException("Unknown locale '" + locale + "'; use e.g. en, pl, de-DE");
        return result;
    }

    static List<String> path(String name) {
        String path = name == null ? "" : name.trim();
        if (path.startsWith("$."))
            path = path.substring(2);
        List<String> nodes = List.of(path.split("\\.", -1));
        if (path.isEmpty() || nodes.stream().anyMatch(String::isBlank))
            throw new IllegalArgumentException("'" + name + "' is not a property path; use e.g. $.name or $.address.city");
        return nodes;
    }

    private static boolean startsWith(List<String> path, List<String> prefix) {
        return path.size() >= prefix.size() && path.subList(0, prefix.size()).equals(prefix);
    }

    @SuppressWarnings("unchecked")
    private static void put(Map<String, Object> row, List<String> path, Object value) {
        Map<String, Object> node = row;
        for (int i = 0; i < path.size() - 1; i++)
            node = (Map<String, Object>) node.computeIfAbsent(path.get(i), k -> new LinkedHashMap<String, Object>());
        node.put(path.get(path.size() - 1), value);
    }
}
