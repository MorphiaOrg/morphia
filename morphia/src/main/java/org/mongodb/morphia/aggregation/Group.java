package org.mongodb.morphia.aggregation;

public class Group<T, U> {
    private final Accumulator accumulator;
    private final String name;

    private Group(final String name, final Accumulator accumulator) {
        this.name = name;
        this.accumulator = accumulator;
    }

    public static Group field(final String name, Accumulator accumulator) {
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
