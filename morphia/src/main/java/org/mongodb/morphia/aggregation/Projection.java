package org.mongodb.morphia.aggregation;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Projection<T, U> {

    private final String sourceField;
    private final String projectedField;
    private List<Projection> projections;
    private List<Object> arguments;
    private boolean suppressed = false;
    private boolean expression = false;

    private Projection(final String field) {
        this.sourceField = field;
        projectedField = null;
    }

    private Projection(final String field, final String projectedField) {
        this.sourceField = field;
        this.projectedField = "$" + projectedField;
    }

    private Projection(final String field, final Projection projection, final Projection... subsequent) {
        this(field);
        this.projections = new ArrayList<Projection>();
        projections.add(projection);
        projections.addAll(Arrays.asList(subsequent));
    }
    
    private Projection(final String expression, final Object...args) {
        this(expression);
        this.arguments = Arrays.asList(args);
        this.expression = true;
    }

    public static <T, U> Projection<T, U> projection(final String name) {
        return new Projection<T, U>(name);
    }

    public static <T, U> Projection<T, U> projection(final String field, final String projectedField) {
        return new Projection<T, U>(field, projectedField);
    }

    public static <T, U> Projection<T, U> projection(final String field, final Projection projection, final Projection... subsequent) {
        return new Projection<T, U>(field, projection, subsequent);
    }
    
    /** Provides access to arbitrary expressions taking an array of arguments, such as $concat */
    public static <T, U> Projection<T, U> expression(final String operator, final Object... args) {
        return new Projection<T, U>(operator, args);
    }
    
    public static <T, U> Projection<T, U> list(final Object... args) {
        return new Projection<T, U>(null, args);
    }
    
    public static <T, U> Projection<T, U> add(final Object... args) {
        return new Projection<T, U>("$add", args);
    }
    
    public static <T, U> Projection<T, U> subtract(final Object arg1, final Object arg2) {
        return new Projection<T, U>("$subtract", arg1, arg2);
    }
    
    public static <T, U> Projection<T, U> multiply(final Object... args) {
        return new Projection<T, U>("$multiply", args);
    }
    
    public static <T, U> Projection<T, U> divide(final Object arg1, final Object arg2) {
        return new Projection<T, U>("$divide", arg1, arg2);
    }
    
    public static <T, U> Projection<T, U> mod(final Object arg1, final Object arg2) {
        return new Projection<T, U>("$mod", arg1, arg2);
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

    public List<Projection> getProjections() {
        return projections;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Projection{");
        sb.append("projectedField='").append(projectedField).append('\'');
        sb.append(", sourceField='").append(sourceField).append('\'');
        sb.append(", projections=").append(projections);
        sb.append(", suppressed=").append(suppressed);
        sb.append('}');
        return sb.toString();
    }
}
