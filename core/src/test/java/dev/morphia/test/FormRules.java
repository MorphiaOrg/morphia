package dev.morphia.test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonRegularExpression;
import org.bson.BsonString;
import org.bson.BsonValue;

/**
 * The declared form rules of the operator audits, in Java: each maps every spelling in an equivalence class to one
 * canonical spelling and is applied to both sides of a comparison, for the rules a scenario declares. The golden
 * vectors in {@code vectors/rules.json} pin each rule's results, including the forms it must leave alone.
 * {@code ALTERNATIVE_COMMANDS} selects among documented commands rather than rewriting one, so it is not a rule here.
 */
public final class FormRules {
    public enum Rule {
        COND_ARRAY,
        EQ_SHORTHAND,
        REGEX_FORM,
        FIELD_OPERATORS_AND,
        SINGLE_ARGUMENT_ARRAY,
        PROJECT_ID_FIRST,
        PROJECTION_FLAGS,
        UNWIND_PATH,
        REGEX_OPTIONS,
        NAMED_ARGUMENTS,
        GEO_COORDINATES
    }

    /** How the action's documents are read. */
    public enum Shape {
        PIPELINE,
        FIND,
        UPDATE
    }

    private static final Set<String> NAMED_ARGUMENT_OPERATORS = Set.of("$bottom", "$bottomN", "$convert", "$dateAdd", "$dateDiff",
            "$dateFromParts",
            "$dateFromString",
            "$dateSubtract", "$dateToString", "$dateTrunc", "$densify", "$fill", "$filter", "$firstN", "$graphLookup", "$lastN", "$let",
            "$lookup", "$ltrim", "$map", "$maxN", "$merge", "$minN", "$reduce", "$regexFind", "$regexFindAll", "$regexMatch", "$rtrim",
            "$setField", "$setWindowFields", "$sortArray", "$text", "$top", "$topN", "$trim", "$unionWith", "$unsetField", "$unwind",
            "$zip");

    /**
     * The arguments that hold coordinates: a driver {@code Point}'s {@code Position} holds doubles, so Morphia sends
     * {@code -88.0} where a page writes {@code -88}, and the server reads both alike.
     */
    private static final Set<String> GEO_POINT_LISTS = Set.of("$box", "$polygon");

    /** The legacy circles: a center point, then a radius, which keeps its type. */
    private static final Set<String> GEO_CIRCLES = Set.of("$center", "$centerSphere");

    private static final Set<String> REGEX_OPERATORS = Set.of("$regexFind", "$regexFindAll", "$regexMatch");

    private static final Set<String> SINGLE_ARGUMENT_OPERATORS = Set.of("$abs", "$allElementsTrue", "$anyElementTrue", "$ceil", "$exp",
            "$floor", "$isArray",
            "$isNumber", "$ln", "$log10", "$not", "$size", "$sqrt", "$strLenBytes", "$strLenCP", "$toBool", "$toDate", "$toDecimal",
            "$toDouble", "$toInt", "$toLong", "$toLower", "$toObjectId", "$toString", "$toUpper", "$year");

    private static final Set<String> COMPARISON_OPERATORS = Set.of("$all", "$eq", "$exists", "$gt", "$gte", "$in", "$lt",
            "$lte", "$mod", "$ne", "$nin", "$size", "$type");

    private final Set<Rule> rules;

    private FormRules(Set<Rule> rules) {
        this.rules = rules;
    }

    /**
     * @return the canonical form of {@code action} under {@code rules}; the input is not modified
     */
    public static List<BsonValue> canonical(List<BsonValue> action, Shape shape, Set<Rule> rules) {
        FormRules canonicalizer = new FormRules(rules);
        List<BsonValue> result = new ArrayList<>();
        for (int i = 0; i < action.size(); i++) {
            BsonValue value = action.get(i);
            result.add(switch (shape) {
                case PIPELINE -> canonicalizer.stage(value);
                case FIND -> i == 0 ? canonicalizer.filter(value) : canonicalizer.projection(value);
                case UPDATE -> i == 0 ? canonicalizer.filter(value) : canonicalizer.expression(value, false);
            });
        }
        return result;
    }

    /**
     * @return the canonical form of a command's {@code options} under {@code rules}: an update's array filters are
     *         filters, and every other option compares as written; the input is not modified
     */
    public static BsonDocument options(BsonDocument options, Set<Rule> rules) {
        FormRules canonicalizer = new FormRules(rules);
        BsonDocument result = options.clone();
        if (result.isArray("arrayFilters")) {
            BsonArray filters = new BsonArray();
            result.getArray("arrayFilters").forEach(filter -> filters.add(canonicalizer.filter(filter)));
            result.put("arrayFilters", filters);
        }
        return result;
    }

    private BsonValue pipeline(BsonValue value) {
        if (value instanceof BsonArray array) {
            BsonArray result = new BsonArray();
            array.forEach(stage -> result.add(stage(stage)));
            return result;
        }
        return expression(value, false);
    }

    private BsonValue stage(BsonValue value) {
        if (!(value instanceof BsonDocument document)) {
            return expression(value, false);
        }
        BsonDocument result = new BsonDocument();
        for (Map.Entry<String, BsonValue> entry : document.entrySet()) {
            String key = entry.getKey();
            BsonValue argument = entry.getValue();
            result.put(key, switch (key) {
                case "$match" -> filter(argument);
                case "$project" -> projection(argument);
                case "$facet" -> {
                    if (argument instanceof BsonDocument facets) {
                        BsonDocument copy = new BsonDocument();
                        facets.forEach((name, stages) -> copy.put(name, pipeline(stages)));
                        yield copy;
                    }
                    yield expression(argument, false);
                }
                case "$unwind" -> named(key, rules.contains(Rule.UNWIND_PATH) && argument instanceof BsonString
                        ? new BsonDocument("path", argument)
                        : expression(argument, false));
                case "$lookup", "$unionWith" -> {
                    if (argument instanceof BsonDocument fields) {
                        BsonDocument copy = new BsonDocument();
                        fields.forEach((name, field) -> copy.put(name,
                                name.equals("pipeline") ? pipeline(field) : expression(field, false)));
                        yield named(key, copy);
                    }
                    yield expression(argument, false);
                }
                case "$geoNear" -> {
                    BsonValue converted = expression(argument, false);
                    // The point a $geoNear stage measures from is GeoJSON, too.
                    if (rules.contains(Rule.GEO_COORDINATES) && converted instanceof BsonDocument stage
                            && stage.get("near") instanceof BsonDocument near) {
                        BsonDocument copy = stage.clone();
                        copy.put("near", geoJson(near));
                        converted = copy;
                    }
                    yield named(key, converted);
                }
                default -> named(key, expression(argument, false));
            });
        }
        return result;
    }

    private BsonValue projection(BsonValue value) {
        if (!(value instanceof BsonDocument document)) {
            return expression(value, false);
        }
        List<Map.Entry<String, BsonValue>> entries = new ArrayList<>();
        document.forEach((key, field) -> entries.add(Map.entry(key,
                rules.contains(Rule.PROJECTION_FLAGS) && field instanceof BsonBoolean flag
                        ? new BsonInt32(flag.getValue() ? 1 : 0)
                        : expression(field, false))));
        if (rules.contains(Rule.PROJECT_ID_FIRST)) {
            // A stable sort, so every other field keeps its position.
            entries.sort(Comparator.comparingInt(entry -> entry.getKey().equals("_id") ? 0 : 1));
        }
        BsonDocument result = new BsonDocument();
        entries.forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    private BsonValue filter(BsonValue value) {
        if (!(value instanceof BsonDocument document)) {
            return expression(value, false);
        }
        if (rules.contains(Rule.FIELD_OPERATORS_AND) && document.size() == 1) {
            String field = document.getFirstKey();
            BsonValue predicate = document.get(field);
            if (!field.startsWith("$") && predicate instanceof BsonDocument operators && operators.size() > 1
                    && COMPARISON_OPERATORS.containsAll(operators.keySet())) {
                BsonArray clauses = new BsonArray();
                operators.forEach((operator, operand) -> clauses
                        .add(filter(new BsonDocument(field, new BsonDocument(operator, operand)))));
                return new BsonDocument("$and", clauses);
            }
        }
        BsonDocument result = new BsonDocument();
        document.forEach((field, predicate) -> {
            if (Set.of("$and", "$or", "$nor").contains(field) && predicate instanceof BsonArray clauses) {
                BsonArray copy = new BsonArray();
                clauses.forEach(clause -> copy.add(filter(clause)));
                result.put(field, copy);
            } else if (field.startsWith("$")) {
                result.put(field, named(field, expression(predicate, false)));
            } else {
                result.put(field, predicate(predicate));
            }
        });
        return result;
    }

    private BsonValue predicate(BsonValue value) {
        BsonValue result = expression(value, true);
        if (rules.contains(Rule.EQ_SHORTHAND) && result instanceof BsonDocument document && document.size() == 1) {
            BsonValue operand = document.get("$eq");
            if (operand != null && !(operand instanceof BsonRegularExpression)
                    && !(operand instanceof BsonDocument nested && !nested.isEmpty() && nested.getFirstKey().startsWith("$"))) {
                result = operand;
            }
        }
        if (rules.contains(Rule.REGEX_FORM)) {
            result = regex(result);
        }
        return result;
    }

    private static BsonValue regex(BsonValue value) {
        String pattern;
        String options;
        if (value instanceof BsonRegularExpression expression) {
            pattern = expression.getPattern();
            options = expression.getOptions();
        } else if (value instanceof BsonDocument document && document.containsKey("$regex")
                && Set.of("$regex", "$options").containsAll(document.keySet())) {
            BsonValue separateValue = document.get("$options");
            String separate;
            if (separateValue == null) {
                separate = "";
            } else if (separateValue instanceof BsonString string) {
                separate = string.getValue();
            } else {
                return value;
            }
            BsonValue expression = document.get("$regex");
            if (expression instanceof BsonString string) {
                pattern = string.getValue();
                options = separate;
            } else if (expression instanceof BsonRegularExpression regex) {
                if (!regex.getOptions().isEmpty() && !separate.isEmpty()) {
                    return value;
                }
                pattern = regex.getPattern();
                options = regex.getOptions() + separate;
            } else {
                return value;
            }
        } else {
            return value;
        }
        BsonDocument result = new BsonDocument("$regex", new BsonString(pattern));
        char[] sorted = options.toCharArray();
        java.util.Arrays.sort(sorted);
        if (sorted.length > 0) {
            result.put("$options", new BsonString(new String(sorted)));
        }
        return result;
    }

    /**
     * {@code query} marks query-operator positions, where {@code $size} and friends are query operators rather than
     * single-argument expressions. {@code $literal} content is data and is never canonicalized.
     */
    private BsonValue expression(BsonValue value, boolean query) {
        if (value instanceof BsonDocument document) {
            BsonDocument result = new BsonDocument();
            document.forEach((key, field) -> {
                if (key.equals("$literal")) {
                    result.put(key, field);
                    return;
                }
                BsonValue converted = expression(field, query);
                if (key.equals("$cond") && rules.contains(Rule.COND_ARRAY)) {
                    converted = condition(converted);
                }
                if (!query && rules.contains(Rule.SINGLE_ARGUMENT_ARRAY) && SINGLE_ARGUMENT_OPERATORS.contains(key)
                        && converted instanceof BsonArray array && array.size() == 1 && !(array.get(0) instanceof BsonArray)) {
                    converted = array.get(0);
                }
                if (rules.contains(Rule.REGEX_OPTIONS) && REGEX_OPERATORS.contains(key)) {
                    converted = regexOptions(converted);
                }
                // Geo operators are query operators: elsewhere, the same names are stored fields.
                if (query && rules.contains(Rule.GEO_COORDINATES) && converted instanceof BsonArray array) {
                    if (GEO_CIRCLES.contains(key) && !array.isEmpty()) {
                        BsonArray circle = new BsonArray();
                        circle.add(coordinates(array.get(0)));
                        circle.addAll(array.subList(1, array.size()));
                        converted = circle;
                    } else if (GEO_POINT_LISTS.contains(key)) {
                        converted = coordinates(array);
                    }
                }
                // Only the GeoJSON a geo operator reads; any other field named coordinates is data.
                if (query && rules.contains(Rule.GEO_COORDINATES) && converted instanceof BsonDocument operand) {
                    if (key.equals("$geometry")) {
                        converted = geoJson(operand);
                    }
                }
                result.put(key, named(key, converted));
            });
            return result;
        }
        if (value instanceof BsonArray array) {
            BsonArray result = new BsonArray();
            array.forEach(item -> result.add(expression(item, query)));
            return result;
        }
        return value;
    }

    /**
     * A regex expression's argument with its pattern as a string and its flags, from the pattern and the {@code options}
     * key, sorted in {@code options}. Flags in both places are left alone, since the server rejects them.
     */
    private static BsonValue regexOptions(BsonValue value) {
        if (!(value instanceof BsonDocument document)) {
            return value;
        }
        String pattern;
        String inline;
        BsonValue regex = document.get("regex");
        if (regex instanceof BsonRegularExpression expression) {
            pattern = expression.getPattern();
            inline = expression.getOptions();
        } else if (regex instanceof BsonString string && !string.getValue().startsWith("$")) {
            // A $ string is a field path the server evaluates, not a pattern.
            pattern = string.getValue();
            inline = "";
        } else {
            return value;
        }
        BsonValue separateValue = document.get("options");
        String separate;
        if (separateValue == null) {
            separate = "";
        } else if (separateValue instanceof BsonString string && !string.getValue().startsWith("$")) {
            separate = string.getValue();
        } else {
            return value;
        }
        if (!inline.isEmpty() && !separate.isEmpty()) {
            return value;
        }
        char[] sorted = (inline + separate).toCharArray();
        java.util.Arrays.sort(sorted);
        String options = new String(sorted);
        BsonDocument result = new BsonDocument();
        document.forEach((name, field) -> {
            if (name.equals("regex")) {
                result.put(name, new BsonString(pattern));
            } else if (name.equals("options")) {
                if (!options.isEmpty()) {
                    result.put(name, new BsonString(options));
                }
            } else {
                result.put(name, field);
            }
        });
        if (!options.isEmpty() && !result.containsKey("options")) {
            result.put("options", new BsonString(options));
        }
        return result;
    }

    /** A GeoJSON object with its coordinates as doubles. */
    private static BsonDocument geoJson(BsonDocument value) {
        BsonDocument result = value.clone();
        if (result.get("coordinates") instanceof BsonArray coordinates) {
            result.put("coordinates", coordinates(coordinates));
        }
        return result;
    }

    /**
     * Every whole number in a coordinate array, at any depth, as the double a {@code Position} holds, where the double
     * holds it exactly.
     */
    private static BsonValue coordinates(BsonValue value) {
        if (value instanceof BsonArray array) {
            BsonArray result = new BsonArray();
            array.forEach(item -> result.add(coordinates(item)));
            return result;
        }
        if (value instanceof BsonInt32 number) {
            return new BsonDouble(number.getValue());
        }
        // Only a long a double holds exactly; two longs must never compare equal as one double.
        if (value instanceof BsonInt64 number && Math.abs(number.getValue()) <= 1L << 53) {
            return new BsonDouble(number.getValue());
        }
        return value;
    }

    private static BsonValue condition(BsonValue value) {
        if (value instanceof BsonDocument document && document.keySet().equals(Set.of("if", "then", "else"))) {
            return new BsonArray(List.of(document.get("if"), document.get("then"), document.get("else")));
        }
        return value;
    }

    private BsonValue named(String key, BsonValue value) {
        if (rules.contains(Rule.NAMED_ARGUMENTS) && NAMED_ARGUMENT_OPERATORS.contains(key)
                && value instanceof BsonDocument document) {
            BsonDocument sorted = new BsonDocument();
            new TreeSet<>(document.keySet()).forEach(name -> sorted.put(name, document.get(name)));
            return sorted;
        }
        return value;
    }
}
