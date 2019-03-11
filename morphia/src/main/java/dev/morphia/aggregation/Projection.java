package dev.morphia.aggregation;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines a projection for use in aggregation
 *
 * @mongodb.driver.manual reference/operator/aggregation/project/ $project
 */
public final class  Projection {

    private final String target;
    private final String source;
    private List<Projection> projections;
    private List<Object> arguments;
    private boolean suppressed = false;

    private Projection(final String field, final String source) {
        this.target = field;
        this.source = "$" + source;
    }

    private Projection(final String field, final Projection projection, final Projection... subsequent) {
        this(field);
        this.projections = new ArrayList<Projection>();
        projections.add(projection);
        projections.addAll(Arrays.asList(subsequent));
    }

    private Projection(final String field) {
        this.target = field;
        source = null;
    }

    private Projection(final String expression, final Object... args) {
        this(expression);
        this.arguments = Arrays.asList(args);
    }

    /**
     * Creates a projection on a field
     *
     * @param field the field
     * @return the projection
     */
    public static  Projection projection(final String field) {
        return new Projection(field);
    }

    /**
     * Creates a projection on a field and renames it
     *
     * @param field          the field
     * @param projectedField the new field name
     * @return the projection
     */
    public static  Projection projection(final String field, final String projectedField) {
        return new Projection(field, projectedField);
    }

    /**
     * Creates a projection on a field with subsequent projects applied.
     *
     * @param field      the field
     * @param projection the project to apply
     * @param subsequent the other projections to apply
     * @return the projection
     */
    public static  Projection projection(final String field, final Projection projection, final Projection... subsequent) {
        return new Projection(field, projection, subsequent);
    }

    /**
     * Provides access to arbitrary expressions taking an array of arguments, such as $concat
     *
     * @param operator the operator for the projection
     * @param args     the projection arguments
     * @return the projection
     */
    public static  Projection expression(final String operator, final Object... args) {
        return new Projection(operator, args);
    }

    /**
     * Creates a list projection
     *
     * @param args the projection arguments
     * @return the projection
     */
    public static  Projection list(final Object... args) {
        return new Projection(null, args);
    }

    /**
     * Creates an addition projection
     *
     * @param args the projection arguments
     * @return the projection
     * @mongodb.driver.manual reference/operator/aggregation/add $add
     */
    public static  Projection add(final Object... args) {
        return expression("$add", args);
    }

    /**
     * Creates a subtraction projection
     *
     * @param arg1 subtraction argument
     * @param arg2 subtraction argument
     * @return the projection
     * @mongodb.driver.manual reference/operator/aggregation/subtract $subtract
     */
    public static  Projection subtract(final Object arg1, final Object arg2) {
        return expression("$subtract", arg1, arg2);
    }

    /**
     * Creates a multiplication projection
     *
     * @param args the projection arguments
     * @return the projection
     * @mongodb.driver.manual reference/operator/aggregation/multiply $multiply
     */
    public static  Projection multiply(final Object... args) {
        return expression("$multiply", args);
    }

    /**
     * Creates a division projection
     *
     * @param arg1 subtraction argument
     * @param arg2 subtraction argument
     * @return the projection
     * @mongodb.driver.manual reference/operator/aggregation/divide $divide
     */
    public static  Projection divide(final Object arg1, final Object arg2) {
        return expression("$divide", arg1, arg2);
    }

    /**
     * Counts and returns the total the number of items in an array
     *
     * @param expression The argument for $size can be any expression as long as it resolves to an array.
     * @return the projection
     * @mongodb.driver.manual reference/operator/aggregation/size $size
     */
    public static Projection size(final Object expression) {
        return expression("$size", expression);
    }

    /**
     * Creates a modulo projection
     *
     * @param arg1 subtraction argument
     * @param arg2 subtraction argument
     * @return the projection
     * @mongodb.driver.manual reference/operator/aggregation/mod $mod
     */
    public static  Projection mod(final Object arg1, final Object arg2) {
        return expression("$mod", arg1, arg2);
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
    public String getSource() {
        return source;
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
    public String getTarget() {
        return target;
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
    public Projection suppress() {
        suppressed = true;
        return this;
    }

    @Override
    public String toString() {
        return String.format("Projection{projectedField='%s', sourceField='%s', projections=%s, suppressed=%s}",
                             source, target, projections, suppressed);
    }
}
