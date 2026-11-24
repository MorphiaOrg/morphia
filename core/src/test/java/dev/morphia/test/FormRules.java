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
import org.bson.BsonInt32;
import org.bson.BsonRegularExpression;
import org.bson.BsonString;
import org.bson.BsonValue;

/**
 * The declared form rules of the operator audits, in Java: each maps every spelling in an equivalence class to one
 * canonical spelling and is applied to both sides of a comparison. This is a port of the audits module's
 * {@code FormEquivalence}; the golden vectors in {@code generated/vectors/rules.json} hold both to the same results.
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
        NAMED_ARGUMENTS
    }

    /** How the action's documents are read. */
    public enum Shape {
        PIPELINE,
        FIND,
        UPDATE
    }

    private static final Set<String> NAMED_ARGUMENT_OPERATORS = Set.of("$bottom", "$bottomN", "$dateAdd", "$dateDiff",
            "$dateFromString", "$dateSubtract", "$dateToString", "$dateTrunc", "$densify", "$fill", "$filter", "$firstN",
            "$graphLookup", "$lastN", "$let", "$lookup", "$map", "$maxN", "$merge", "$minN", "$reduce", "$regexFind", "$regexFindAll",
            "$regexMatch", "$sortArray", "$top", "$topN", "$unwind", "$zip");

    private static final Set<String> SINGLE_ARGUMENT_OPERATORS = Set.of("$abs", "$ceil", "$exp", "$floor", "$isArray",
            "$isNumber", "$ln", "$log10", "$size", "$sqrt", "$strLenBytes", "$strLenCP", "$toBool", "$toDate", "$toDecimal",
            "$toDouble", "$toInt", "$toLong", "$toLower", "$toObjectId", "$toString", "$toUpper");

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
                result.put(field, expression(predicate, false));
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
