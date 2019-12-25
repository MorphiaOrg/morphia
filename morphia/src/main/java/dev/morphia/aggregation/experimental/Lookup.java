package dev.morphia.aggregation.experimental;

import dev.morphia.aggregation.experimental.stages.Stage;

public class Lookup extends Stage {
    private Class<?> source;
    private String localField;
    private String foreignField;
    private String as;

    protected Lookup(final Class<?> source) {
        super("$lookup");
        this.source = source;
    }

    public Class<?> getSource() {
        return source;
    }

    public String getLocalField() {
        return localField;
    }

    public Lookup localField(final String localField) {
        this.localField = localField;
        return this;
    }

    public String getForeignField() {
        return foreignField;
    }

    public Lookup foreignField(final String foreignField) {
        this.foreignField = foreignField;
        return this;
    }

    public String getAs() {
        return as;
    }

    public Lookup as(final String as) {
        this.as = as;
        return this;
    }

    public static Lookup from(Class<?> source) {
        return new Lookup(source);
    }
}
