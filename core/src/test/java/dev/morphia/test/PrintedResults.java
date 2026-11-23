package dev.morphia.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.BsonSymbol;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.types.Decimal128;

/**
 * Checks results against what a page's printed results prove. mongosh prints less than BSON holds: a printed
 * {@code 4} may be an Int32 or a Double, and printed text may be a string or a symbol, so a printed result is a
 * constraint, not a value. This is a port of the audits module's {@code PublishedComparison} over the encoding its
 * {@code PublishedEncoding} writes: each node is a one-key document naming its kind, {@code {document: [{name,
 * value}]}}, {@code {array: [..]}}, {@code {exact: v}}, {@code {whole: n}}, {@code {text: s}}, {@code {nan: true}}
 * or {@code {decimal: s}}. The golden vectors in {@code generated/vectors/printed.json} hold both to the same
 * verdicts. The port certifies verdicts for well-formed constraints over results from the driver: it does not repeat
 * the audits' work and depth limits or their checks for unpaired surrogates and non-plain documents, which guard
 * against untrusted input, while scenarios are generated and reviewed.
 */
public final class PrintedResults {
    private PrintedResults() {
    }

    /**
     * @param ordered whether position matters; if not, each result must match a distinct constraint
     * @return why {@code actual} does not satisfy {@code constraints}, or null when it does
     * @throws IllegalArgumentException when the constraints are malformed
     */
    public static String difference(List<BsonValue> constraints, List<BsonValue> actual, boolean ordered) {
        return difference(constraints, actual, ordered, Set.of());
    }

    /**
     * @param ordered         whether position matters; if not, each result must match a distinct constraint
     * @param unorderedArrays the pointers, relative to each result, of the arrays whose printed items each need a
     *                        distinct matching item, in any order
     * @return why {@code actual} does not satisfy {@code constraints}, or null when it does
     * @throws IllegalArgumentException when the constraints are malformed
     */
    public static String difference(List<BsonValue> constraints, List<BsonValue> actual, boolean ordered,
            Set<String> unorderedArrays) {
        constraints.forEach(PrintedResults::validate);
        if (constraints.size() != actual.size()) {
            return "result counts differ";
        }
        if (ordered) {
            for (int i = 0; i < constraints.size(); i++) {
                // Paths are relative to the row, as the unordered arrays are, then placed in it.
                String difference = difference(constraints.get(i), actual.get(i), "", unorderedArrays);
                if (difference != null) {
                    return "/" + i + difference;
                }
            }
            return null;
        }
        // Constraints overlap: a constraint matching an Int32 or a Double must not take the only match of an exact
        // Double, so this finds a maximum matching rather than taking the first match.
        int unmatched = unmatched(constraints.size(), actual.size(),
                (left, right) -> difference(constraints.get(left), actual.get(right), "", unorderedArrays) == null);
        return unmatched < 0 ? null : "/" + unmatched + ": no result matches this constraint";
    }

    /** @return the first left index a maximum bipartite matching leaves unmatched, or -1 when none is */
    private static int unmatched(int lefts, int rights, BiPredicate<Integer, Integer> candidate) {
        boolean[][] matches = new boolean[lefts][rights];
        for (int left = 0; left < lefts; left++) {
            for (int right = 0; right < rights; right++) {
                matches[left][right] = candidate.test(left, right);
            }
        }
        int[] owners = new int[rights];
        Arrays.fill(owners, -1);
        for (int left = 0; left < lefts; left++) {
            if (!assign(left, matches, owners, new boolean[rights])) {
                return left;
            }
        }
        return -1;
    }

    /**
     * @return {@code constraint}, a printed document, without the top-level {@code fields}
     */
    public static BsonValue without(Set<String> fields, BsonValue constraint) {
        BsonDocument document = constraint.asDocument();
        if (fields.isEmpty() || !document.containsKey("document")) {
            return constraint;
        }
        BsonArray kept = new BsonArray();
        document.getArray("document")
                .forEach(field -> {
                    if (!fields.contains(field.asDocument().getString("name").getValue())) {
                        kept.add(field);
                    }
                });
        return new BsonDocument("document", kept);
    }

    private static boolean assign(int left, boolean[][] matches, int[] owners, boolean[] visited) {
        for (int right = 0; right < owners.length; right++) {
            if (matches[left][right] && !visited[right]) {
                visited[right] = true;
                if (owners[right] == -1 || assign(owners[right], matches, owners, visited)) {
                    owners[right] = left;
                    return true;
                }
            }
        }
        return false;
    }

    private static void validate(BsonValue node) {
        if (!(node instanceof BsonDocument document) || document.size() != 1) {
            throw new IllegalArgumentException("A printed constraint is a one-key document: " + node);
        }
        BsonValue argument = document.get(document.getFirstKey());
        switch (document.getFirstKey()) {
            case "document" -> {
                List<String> names = new ArrayList<>();
                for (BsonValue value : argument.asArray()) {
                    BsonDocument field = value.asDocument();
                    if (!field.keySet().equals(Set.of("name", "value"))) {
                        throw new IllegalArgumentException("A printed field is {name, value}: " + field);
                    }
                    String name = field.getString("name").getValue();
                    if (name.indexOf('\0') >= 0) {
                        throw new IllegalArgumentException("A printed field name contains NUL");
                    }
                    names.add(name);
                    validate(field.get("value"));
                }
                if (new HashSet<>(names).size() != names.size()) {
                    throw new IllegalArgumentException("Duplicate printed field in " + names);
                }
                if (names.contains("$ref") && names.contains("$id")) {
                    throw new IllegalArgumentException("A DBRef-shaped printed document needs an explicit rule");
                }
                if (!javaScriptOrder(names).equals(names)) {
                    throw new IllegalArgumentException("Printed fields must be in ECMAScript own-property order: " + names);
                }
            }
            case "array" -> argument.asArray().forEach(PrintedResults::validate);
            case "exact" -> {
                BsonType type = argument.getBsonType();
                if (type == BsonType.DOCUMENT || type == BsonType.ARRAY || type == BsonType.JAVASCRIPT_WITH_SCOPE) {
                    throw new IllegalArgumentException("An exact printed value is a scalar: " + argument);
                }
            }
            case "whole" -> argument.asInt32();
            case "text" -> argument.asString();
            case "nan" -> {
                if (!argument.equals(BsonBoolean.TRUE)) {
                    throw new IllegalArgumentException("nan takes true");
                }
            }
            case "decimal" -> {
                String text = argument.asString().getValue();
                if (text.length() > 1024 || !Decimal128.parse(text).toString().equals(text)) {
                    throw new IllegalArgumentException("Not a printed Decimal128: " + text);
                }
            }
            default -> throw new IllegalArgumentException("Unknown printed constraint " + document.getFirstKey());
        }
    }

    private static String difference(BsonValue node, BsonValue actual, String path, Set<String> unordered) {
        BsonDocument constraint = node.asDocument();
        String kind = constraint.getFirstKey();
        BsonValue argument = constraint.get(kind);
        String mismatch = path + ": printed " + kind + " " + argument + " differs from " + actual;
        switch (kind) {
            case "document" -> {
                if (!(actual instanceof BsonDocument document)) {
                    return mismatch;
                }
                List<String> names = new ArrayList<>();
                argument.asArray().forEach(field -> names.add(field.asDocument().getString("name").getValue()));
                for (String name : names) {
                    if (!document.containsKey(name)) {
                        return path + "/" + name + ": missing field";
                    }
                }
                for (String key : document.keySet()) {
                    if (!names.contains(key)) {
                        return path + "/" + key + ": unexpected field";
                    }
                }
                // mongosh prints fields as JavaScript enumerates them, so the printed order is the result's keys in
                // that order.
                if (!names.equals(javaScriptOrder(new ArrayList<>(document.keySet())))) {
                    return path + ": key order differs";
                }
                for (BsonValue value : argument.asArray()) {
                    BsonDocument field = value.asDocument();
                    String name = field.getString("name").getValue();
                    String difference = difference(field.get("value"), document.get(name), child(path, name), unordered);
                    if (difference != null) {
                        return difference;
                    }
                }
                return null;
            }
            case "array" -> {
                if (!(actual instanceof BsonArray array) || array.size() != argument.asArray().size()) {
                    return mismatch;
                }
                if (unordered.contains(path)) {
                    List<BsonValue> items = argument.asArray().getValues();
                    int unmatched = unmatched(items.size(), array.size(),
                            (left, right) -> difference(items.get(left), array.get(right), child(path, String.valueOf(left)),
                                    unordered) == null);
                    return unmatched < 0 ? null
                            : child(path, String.valueOf(unmatched)) + ": no item of the unordered array satisfies this constraint";
                }
                for (int i = 0; i < array.size(); i++) {
                    String difference = difference(argument.asArray().get(i), array.get(i), child(path, String.valueOf(i)),
                            unordered);
                    if (difference != null) {
                        return difference;
                    }
                }
                return null;
            }
            case "whole" -> {
                int value = argument.asInt32().getValue();
                boolean matches = actual instanceof BsonInt32 integer && integer.getValue() == value
                        || actual instanceof BsonDouble real
                                && Double.doubleToRawLongBits(real.getValue()) == Double.doubleToRawLongBits(value);
                return matches ? null : mismatch;
            }
            case "text" -> {
                String value = argument.asString().getValue();
                boolean matches = actual instanceof BsonString string && string.getValue().equals(value)
                        || actual instanceof BsonSymbol symbol && symbol.getSymbol().equals(value);
                return matches ? null : mismatch;
            }
            case "nan" -> {
                return actual instanceof BsonDouble number && Double.isNaN(number.getValue()) ? null : mismatch;
            }
            case "decimal" -> {
                return actual instanceof BsonDecimal128 number
                        && number.getValue().toString().equals(argument.asString().getValue()) ? null : mismatch;
            }
            default -> {
                return StrictBson.equal(argument, actual) ? null : mismatch;
            }
        }
    }

    private static String child(String path, String segment) {
        return path + "/" + segment.replace("~", "~0").replace("/", "~1");
    }

    /** JavaScript own-property order: array-index keys first, ascending, then the others in insertion order. */
    private static List<String> javaScriptOrder(List<String> keys) {
        List<String> result = new ArrayList<>(keys.stream().filter(key -> index(key) >= 0)
                .sorted(Comparator.comparingLong(PrintedResults::index)).toList());
        keys.stream().filter(key -> index(key) < 0).forEach(result::add);
        return result;
    }

    private static long index(String key) {
        if (key.isEmpty() || key.length() > 10 || !key.chars().allMatch(c -> c >= '0' && c <= '9')
                || key.length() > 1 && key.charAt(0) == '0') {
            return -1;
        }
        long value = Long.parseLong(key);
        return value <= 4_294_967_294L ? value : -1;
    }
}
