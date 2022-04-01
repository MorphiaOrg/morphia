package dev.morphia.aggregation;

import com.mongodb.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Defines a group pipeline stage.
 *
 * @aggregation.expression $group
 * @deprecated use {@link dev.morphia.aggregation.stages.Group} instead
 */
@Deprecated(since = "2.0", forRemoval = true)
public final class Group {
    private final String name;
    @Nullable
    private Group nested;
    @Nullable
    private List<Projection> projections;
    @Nullable
    private Accumulator accumulator;
    private String sourceField;

    private Group(String name, Accumulator accumulator) {
        this.name = name;
        this.accumulator = accumulator;
    }

    /**
     * Creates a new Group
     *
     * @param name        the name of the group
     * @param sourceField the source field
     */
    private Group(String name, String sourceField) {
        this.name = name;
        this.sourceField = "$" + sourceField;
    }

    /**
     * Creates a new Group
     *
     * @param name        the name of the group
     * @param projections the fields to create
     */
    private Group(String name, Projection... projections) {
        this.name = name;
        this.projections = asList(projections);
    }

    private Group(String name, Group nested) {
        this.name = name;
        this.nested = nested;
    }

    /**
     * Returns an array of all unique values that results from applying an expression to each document in a group of documents that share
     * the same group by key. Order of the elements in the output array is unspecified.
     *
     * @param field the field to process
     * @return an Accumulator
     * @aggregation.expression $addToSet
     */
    public static Accumulator addToSet(String field) {
        return new Accumulator("$addToSet", field);
    }

    /**
     * Returns the average value of the numeric values that result from applying a specified expression to each document in a group of
     * documents that share the same group by key. $avg ignores non-numeric values.
     *
     * @param field the field to process
     * @return an Accumulator
     * @aggregation.expression $avg
     */
    public static Accumulator average(String field) {
        return new Accumulator("$avg", field);
    }

    /**
     * Returns the value that results from applying an expression to the first document in a group of documents that share the same group
     * by
     * key. Only meaningful when documents are in a defined order.
     *
     * @param field the field to process
     * @return an Accumulator
     * @aggregation.expression $first
     */
    public static Accumulator first(String field) {
        return new Accumulator("$first", field);
    }

    /**
     * Creates a named grouping
     *
     * @param name the field name
     * @return the Group
     */
    public static Group grouping(String name) {
        return grouping(name, name);
    }

    /**
     * Creates a named grouping on a field
     *
     * @param name        the group name
     * @param sourceField the field name
     * @return the Group
     */
    public static Group grouping(String name, String sourceField) {
        return new Group(name, sourceField);
    }

    /**
     * Creates a named grouping
     *
     * @param name        the field name
     * @param projections the fields to create
     * @return the Group
     */
    public static Group grouping(String name, Projection... projections) {
        return new Group(name, projections);
    }

    /**
     * Creates a named grouping
     *
     * @param name  the field name
     * @param group the fields to create
     * @return the Group
     */
    public static Group grouping(String name, Group group) {
        return new Group(name, group);
    }

    /**
     * Creates a named grouping on a field
     *
     * @param name        the group name
     * @param accumulator the Accumulator to apply to the field
     * @return the Group
     */
    public static Group grouping(String name, Accumulator accumulator) {
        return new Group(name, accumulator);
    }

    /**
     * Create a group of Groups for use as an ID
     *
     * @param fields the Groups to group
     * @return the Group
     */
    public static List<Group> id(Group... fields) {
        return asList(fields);
    }

    /**
     * Returns the value that results from applying an expression to the last document in a group of documents that share the same group by
     * a field. Only meaningful when documents are in a defined order.
     *
     * @param field the field to process
     * @return an Accumulator
     * @aggregation.expression $last
     */
    public static Accumulator last(String field) {
        return new Accumulator("$last", field);
    }

    /**
     * Returns the highest value that results from applying an expression to each document in a group of documents that share the same
     * group
     * by key.
     *
     * @param field the field to process
     * @return an Accumulator
     * @aggregation.expression $max
     */
    public static Accumulator max(String field) {
        return new Accumulator("$max", field);
    }

    /**
     * Returns the lowest value that results from applying an expression to each document in a group of documents that share the same group
     * by key.
     *
     * @param field the field to process
     * @return an Accumulator
     * @aggregation.expression $min
     */
    public static Accumulator min(String field) {
        return new Accumulator("$min", field);
    }

    /**
     * Returns an array of all values that result from applying an expression to each document in a group of documents that share the same
     * group by key.
     *
     * @param field the field to process
     * @return an Accumulator
     * @aggregation.expression $push
     */
    public static Accumulator push(String field) {
        return new Accumulator("$push", field);
    }

    /**
     * Calculates and returns the sum of all the numeric values that result from applying a specified expression to each document in a
     * group
     * of documents that share the same group by key. $sum ignores non-numeric values.
     *
     * @param field the field to process
     * @return an Accumulator
     * @aggregation.expression $sum
     */
    public static Accumulator sum(String field) {
        return new Accumulator("$sum", field);
    }

    /**
     * @return the accumulator for this Group
     */
    @Nullable
    public Accumulator getAccumulator() {
        return accumulator;
    }

    /**
     * @return the name of the group
     */
    public String getName() {
        return name;
    }

    /**
     * @return the nested group
     */
    @Nullable
    public Group getNested() {
        return nested;
    }

    /**
     * @return the projections for the group
     */
    @Nullable
    public List<Projection> getProjections() {
        return projections != null ? new ArrayList<>(projections) : null;
    }

    /**
     * @return the source field for the group
     */
    public String getSourceField() {
        return sourceField;
    }
}
