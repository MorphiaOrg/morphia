package org.mongodb.morphia.aggregation;

import java.util.Arrays;
import java.util.List;

public class Group<T, U> {
	
    private IAccumulator accumulator;
    private final String name;
    private String sourceField;

    private Group(final String name, final IAccumulator accumulator) {
        this.name = name;
        this.accumulator = accumulator;
    }

    public Group(final String name, final String sourceField) {
        this.name = name;
        this.sourceField = "$" + sourceField;
    }

    public IAccumulator getAccumulator() {
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
    
    public static Group grouping(final String name, final IAccumulator accumulator) {
        return new Group(name, accumulator);
    }

    public static IAccumulator addToSet(final String field) {
        return new Accumulator("$addToSet", field);
    }

    public static IAccumulator average(final String field) {
        return new Accumulator("$avg", field);
    }

    public static IAccumulator first(final String field) {
        return new Accumulator("$first", field);
    }

    public static IAccumulator last(final String field) {
        return new Accumulator("$last", field);
    }

    public static IAccumulator max(final String field) {
        return new Accumulator("$max", field);
    }

    public static IAccumulator min(final String field) {
        return new Accumulator("$min", field);
    }

    public static IAccumulator push(final String field) {
        return new Accumulator("$push", field);
    }

    public static IAccumulator sum(final String field) {
        return new Accumulator("$sum", field);
    }
}
