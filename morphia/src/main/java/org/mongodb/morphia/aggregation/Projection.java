package org.mongodb.morphia.aggregation;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines a projection for use in aggregation
 *
 * @param <T> the source type
 * @param <U> the target type
 * @mongodb.driver.manual reference/operator/aggregation/project/ $project
 */
public final class Projection<T, U> {

    private final String sourceField;
    private final String projectedField;
    private List<Projection> projections;
    private List<Object> arguments;
    private boolean suppressed = false;
    private boolean expression = false;

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

    private Projection(final String field) {
        this.sourceField = field;
        projectedField = null;
    }

    private Projection(final String expression, final Object... args) {
        this(expression);
        this.arguments = Arrays.asList(args);
        this.expression = true;
    }

    /**
     * Creates a projection on a field
     *
     * @param field the field
     * @param <T>   the source type
     * @param <U>   the target type
     * @return the projection
     */
    public static <T, U> Projection<T, U> projection(final String field) {
        return new Projection<T, U>(field);
    }

    /**
     * Creates a projection on a field and renames it
     *
     * @param field          the field
     * @param projectedField the new field name
     * @param <T>            the source type
     * @param <U>            the target type
     * @return the projection
     */
    public static <T, U> Projection<T, U> projection(final String field, final String projectedField) {
        return new Projection<T, U>(field, projectedField);
    }

    /**
     * Creates a projection on a field with subsequent projects applied.
     *
     * @param field      the field
     * @param projection the project to apply
     * @param subsequent the other projections to apply
     * @param <T>        the source type
     * @param <U>        the target type
     * @return the projection
     */
    public static <T, U> Projection<T, U> projection(final String field, final Projection projection, final Projection... subsequent) {
        return new Projection<T, U>(field, projection, subsequent);
    }

    /**
     * Provides access to arbitrary expressions taking an array of arguments, such as $concat
     *
     * @param operator the operator for the projection
     * @param args     the projection arguments
     * @param <T>      the source type
     * @param <U>      the target type
     * @return the projection
     */
    public static <T, U> Projection<T, U> expression(final String operator, final Object... args) {
        return new Projection<T, U>(operator, args);
    }

    /**
     * Creates a list projection
     *
     * @param args the projection arguments
     * @param <T>  the source type
     * @param <U>  the target type
     * @return the projection
     */
    public static <T, U> Projection<T, U> list(final Object... args) {
        return new Projection<T, U>(null, args);
    }

    /**
     * Creates an addition projection
     *
     * @param args the projection arguments
     * @param <T>  the source type
     * @param <U>  the target type
     * @return the projection
     * @mongodb.driver.manual reference/operator/aggregation/add $add
     */
    public static <T, U> Projection<T, U> add(final Object... args) {
        return new Projection<T, U>("$add", args);
    }

    /**
     * Creates a subtraction projection
     *
     * @param arg1 subtraction argument
     * @param arg2 subtraction argument
     * @param <T>  the source type
     * @param <U>  the target type
     * @return the projection
     * @mongodb.driver.manual reference/operator/aggregation/subtract $subtract
     */
    public static <T, U> Projection<T, U> subtract(final Object arg1, final Object arg2) {
        return new Projection<T, U>("$subtract", arg1, arg2);
    }

    /**
     * Creates a multiplication projection
     *
     * @param args the projection arguments
     * @param <T>  the source type
     * @param <U>  the target type
     * @return the projection
     * @mongodb.driver.manual reference/operator/aggregation/multiply $multiply
     */
    public static <T, U> Projection<T, U> multiply(final Object... args) {
        return new Projection<T, U>("$multiply", args);
    }

    /**
     * Creates a division projection
     *
     * @param arg1 subtraction argument
     * @param arg2 subtraction argument
     * @param <T>  the source type
     * @param <U>  the target type
     * @return the projection
     * @mongodb.driver.manual reference/operator/aggregation/divide $divide
     */
    public static <T, U> Projection<T, U> divide(final Object arg1, final Object arg2) {
        return new Projection<T, U>("$divide", arg1, arg2);
    }

    /**
     * Creates a modulo projection
     *
     * @param arg1 subtraction argument
     * @param arg2 subtraction argument
     * @param <T>  the source type
     * @param <U>  the target type
     * @return the projection
     * @mongodb.driver.manual reference/operator/aggregation/mod $mod
     */
    public static <T, U> Projection<T, U> mod(final Object arg1, final Object arg2) {
        return new Projection<T, U>("$mod", arg1, arg2);
    }

    /**
     * @return the arguments for the projection
     */
    public List<Object> getArguments() {
        return arguments;
    }

    /**
     * @return the projected field name
     */
    public String getProjectedField() {
        return projectedField;
    }

    /**
     * @return any projections applied to this field
     */
    public List<Projection> getProjections() {
        return projections;
    }

    /**
     * @return the source field of the projection
     */
    public String getSourceField() {
        return sourceField;
    }

    /**
     * @return true if this field is suppressed from the output
     */
    public boolean isSuppressed() {
        return suppressed;
    }

    /**
     * Marks this field to be suppressed from the output of this stage
     *
     * @return this
     */
    public Projection<T, U> suppress() {
        suppressed = true;
        return this;
    }

    @Override
    public String toString() {
        return String.format("Projection{projectedField='%s', sourceField='%s', projections=%s, suppressed=%s}", projectedField,
                             sourceField, projections, suppressed);
    }
}
