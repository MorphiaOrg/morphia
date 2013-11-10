package org.mongodb.morphia.aggregation;


public class Projection<T, U> /*implements ExpressionOperation*/ {

    private final String sourceField;
    private final String projectedField;
    private Projection projection;
    private boolean suppressed = false;

    private Projection(final String field) {
        this.sourceField = field;
        projectedField = null;
    }

    private Projection(final String field, final String projectedField) {
        this.sourceField = field;
        this.projectedField = projectedField;
    }

    private Projection(final String field, final Projection projection) {
        this(field);
        this.projection = projection;
    }

    public static <T, U> Projection<T, U> project(final String name) {
        return new Projection<T, U>(name);
    }

    public static <T, U> Projection<T, U> project(final String field, final String projectedField) {
        return new Projection<T, U>(field, projectedField);
    }

    public static <T, U> Projection<T, U> project(final String field, final Projection projection) {
        return new Projection<T, U>(field, projection);
    }

    public Projection<T, U> suppress() {
        suppressed = true;
        return this;
    }

    public String getProjectedField() {
        return projectedField;
    }

    public String getSourceField() {
        return sourceField;
    }

    public boolean isSuppressed() {
        return suppressed;
    }

    public Projection getProjection() {
        return projection;
    }
}
