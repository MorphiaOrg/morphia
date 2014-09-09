package org.mongodb.morphia.aggregation;

import java.util.Arrays;
import java.util.List;

public class Group<T, U> {
    private Accumulator accumulator;
    private final String name;
    private String sourceField;

    private Group(final String name, final Accumulator accumulator) {
        this.name = name;
        this.accumulator = accumulator;
    }

    public Group(final String name, final String sourceField) {
        this.name = name;
        this.sourceField = "$" + sourceField;
    }

    public Accumulator getAccumulator() {
        return accumulator;
    }

    public String getName() {
        return name;
    }

    public String getSourceField() {
        return sourceField;
    }

    public static List<Group> id(final Group... field) {
        return Arrays.asList(field);
    }

    public static Group grouping(final String name) {
        return grouping(name, name);
    }

    public static Group grouping(final String name, final String sourceField) {
        return new Group(name, sourceField);
    }
    
    public static Group grouping(final String name, final Accumulator accumulator) {
        return new Group(name, accumulator);
    }

    public static Accumulator addToSet(final String field) {
        return new Accumulator("$addToSet", field);
    }

    public static Accumulator average(final String field) {
        return new Accumulator("$avg", field);
    }

    public static Accumulator first(final String field) {
        return new Accumulator("$first", field);
    }

    public static Accumulator last(final String field) {
        return new Accumulator("$last", field);
    }

    public static Accumulator max(final String field) {
        return new Accumulator("$max", field);
    }

    public static Accumulator min(final String field) {
        return new Accumulator("$min", field);
    }

    public static Accumulator push(final String field) {
        return new Accumulator("$push", field);
    }

    public static Accumulator sum(final String field) {
        return new Accumulator("$sum", field);
    }
}
