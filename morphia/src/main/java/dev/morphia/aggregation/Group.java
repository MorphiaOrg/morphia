package dev.morphia.aggregation;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Defines a group pipeline stage.
 *
 * @mongodb.driver.manual reference/operator/aggregation/group/ $group
 */
public final class Group {
    private final String name;
    private Group nested;
    private List<Projection> projections;
    private Accumulator accumulator;
    private String sourceField;

    private Group(final String name, final Accumulator accumulator) {
        this.name = name;
        this.accumulator = accumulator;
    }

    /**
     * Creates a new Group
     *
     * @param name        the name of the group
     * @param sourceField the source field
     */
    private Group(final String name, final String sourceField) {
        this.name = name;
        this.sourceField = "$" + sourceField;
    }
    /**
     * Creates a new Group
     *
     * @param name        the name of the group
     * @param projections the fields to create
     */
    private Group(final String name, final Projection... projections) {
        this.name = name;
        this.projections = asList(projections);
    }

    private Group(final String name, final Group nested) {
        this.name = name;
        this.nested = nested;
    }

    /**
     * Create a group of Groups for use as an ID
     *
     * @param fields the Groups to group
     * @return the Group
     */
    public static List<Group> id(final Group... fields) {
        return asList(fields);
    }

    /**
     * Creates a named grouping
     *
     * @param name the field name
     * @return the Group
     */
    public static Group grouping(final String name) {
        return grouping(name, name);
    }

    /**
     * Creates a named grouping
     *
     * @param name        the field name
     * @param projections the fields to create
     * @return the Group
     */
    public static Group grouping(final String name, final Projection... projections) {
        return new Group(name, projections);
    }
    /**
     * Creates a named grouping
     *
     * @param name        the field name
     * @param group the fields to create
     * @return the Group
     */
    public static Group grouping(final String name, final Group group) {
        return new Group(name, group);
    }

    /**
     * Creates a named grouping on a field
     *
     * @param name        the group name
     * @param sourceField the field name
     * @return the Group
     */
    public static Group grouping(final String name, final String sourceField) {
        return new Group(name, sourceField);
    }

    /**
     * Creates a named grouping on a field
     *
     * @param name        the group name
     * @param accumulator the Accumulator to apply to the field
     * @return the Group
     */
    public static Group grouping(final String name, final Accumulator accumulator) {
        return new Group(name, accumulator);
    }

    /**
     * Returns an array of all unique values that results from applying an expression to each document in a group of documents that share
     * the same group by key. Order of the elements in the output array is unspecified.
     *
     * @param field the field to process
     * @return an Accumulator
     * @mongodb.driver.manual reference/operator/aggregation/addToSet $addToSet
     */
    public static Accumulator addToSet(final String field) {
        return new Accumulator("$addToSet", field);
    }

    /**
     * Returns the average value of the numeric values that result from applying a specified expression to each document in a group of
     * documents that share the same group by key. $avg ignores non-numeric values.
     *
     * @param field the field to process
     * @return an Accumulator
     * @mongodb.driver.manual reference/operator/aggregation/avg $avg
     */
    public static Accumulator average(final String field) {
        return new Accumulator("$avg", field);
    }

    /**
     * Returns the value that results from applying an expression to the first document in a group of documents that share the same group
     * by
     * key. Only meaningful when documents are in a defined order.
     *
     * @param field the field to process
     * @return an Accumulator
     * @mongodb.driver.manual reference/operator/aggregation/first $first
     */
    public static Accumulator first(final String field) {
        return new Accumulator("$first", field);
    }

    /**
     * Returns the value that results from applying an expression to the last document in a group of documents that share the same group by
     * a field. Only meaningful when documents are in a defined order.
     *
     * @param field the field to process
     * @return an Accumulator
     * @mongodb.driver.manual reference/operator/aggregation/last $last
     */
    public static Accumulator last(final String field) {
        return new Accumulator("$last", field);
    }

    /**
     * Returns the highest value that results from applying an expression to each document in a group of documents that share the same
     * group
     * by key.
     *
     * @param field the field to process
     * @return an Accumulator
     * @mongodb.driver.manual reference/operator/aggregation/max $max
     */
    public static Accumulator max(final String field) {
        return new Accumulator("$max", field);
    }

    /**
     * Returns the lowest value that results from applying an expression to each document in a group of documents that share the same group
     * by key.
     *
     * @param field the field to process
     * @return an Accumulator
     * @mongodb.driver.manual reference/operator/aggregation/min $min
     */
    public static Accumulator min(final String field) {
        return new Accumulator("$min", field);
    }

    /**
     * Returns an array of all values that result from applying an expression to each document in a group of documents that share the same
     * group by key.
     *
     * @param field the field to process
     * @return an Accumulator
     * @mongodb.driver.manual reference/operator/aggregation/push $push
     */
    public static Accumulator push(final String field) {
        return new Accumulator("$push", field);
    }

    /**
     * Calculates and returns the sum of all the numeric values that result from applying a specified expression to each document in a
     * group
     * of documents that share the same group by key. $sum ignores non-numeric values.
     *
     * @param field the field to process
     * @return an Accumulator
     * @mongodb.driver.manual reference/operator/aggregation/sum $sum
     */
    public static Accumulator sum(final String field) {
        return new Accumulator("$sum", field);
    }

    /**
     * @return the accumulator for this Group
     */
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
     * @return the source field for the group
     */
    public String getSourceField() {
        return sourceField;
    }

    /**
     * @return the projections for the group
     */
    public List<Projection> getProjections() {
        return projections != null ? new ArrayList<Projection>(projections) : null;
    }

    /**
     * @return the nested group
     */
    public Group getNested() {
        return nested;
    }
}
