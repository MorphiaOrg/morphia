package dev.morphia.aggregation;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines a projection for use in aggregation
 *
 * @aggregation.expression $project
 * @deprecated use {@link dev.morphia.aggregation.experimental.stages.Projection} instead
 */
@Deprecated(since = "2.0", forRemoval = true)
public final class  Projection {

    private final String target;
    private final String source;
    private List<Projection> projections;
    private List<Object> arguments;
    private boolean suppressed = false;

    private Projection(String field, String source) {
        this.target = field;
        this.source = "$" + source;
    }

    private Projection(String field, Projection projection, Projection... subsequent) {
        this(field);
        this.projections = new ArrayList<Projection>();
        projections.add(projection);
        projections.addAll(Arrays.asList(subsequent));
    }

    private Projection(String field) {
        this.target = field;
        source = null;
    }

    private Projection(String expression, Object... args) {
        this(expression);
        this.arguments = Arrays.asList(args);
    }

    /**
     * Creates a projection on a field
     *
     * @param field the field
     * @return the projection
     */
    public static  Projection projection(String field) {
        return new Projection(field);
    }

    /**
     * Creates a projection on a field and renames it
     *
     * @param field          the field
     * @param projectedField the new field name
     * @return the projection
     */
    public static  Projection projection(String field, String projectedField) {
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
    public static  Projection projection(String field, Projection projection, Projection... subsequent) {
        return new Projection(field, projection, subsequent);
    }

    /**
     * Provides access to arbitrary expressions taking an array of arguments, such as $concat
     *
     * @param operator the operator for the projection
     * @param args     the projection arguments
     * @return the projection
     */
    public static  Projection expression(String operator, Object... args) {
        return new Projection(operator, args);
    }

    /**
     * Creates a list projection
     *
     * @param args the projection arguments
     * @return the projection
     */
    public static  Projection list(Object... args) {
        return new Projection(null, args);
    }

    /**
     * Creates an addition projection
     *
     * @param args the projection arguments
     * @return the projection
     * @aggregation.expression $add
     */
    public static  Projection add(Object... args) {
        return expression("$add", args);
    }

    /**
     * Creates a subtraction projection
     *
     * @param arg1 subtraction argument
     * @param arg2 subtraction argument
     * @return the projection
     * @aggregation.expression $subtract
     */
    public static  Projection subtract(Object arg1, Object arg2) {
        return expression("$subtract", arg1, arg2);
    }

    /**
     * Creates a multiplication projection
     *
     * @param args the projection arguments
     * @return the projection
     * @aggregation.expression $multiply
     */
    public static  Projection multiply(Object... args) {
        return expression("$multiply", args);
    }

    /**
     * Creates a division projection
     *
     * @param arg1 subtraction argument
     * @param arg2 subtraction argument
     * @return the projection
     * @aggregation.expression $divide
     */
    public static  Projection divide(Object arg1, Object arg2) {
        return expression("$divide", arg1, arg2);
    }

    /**
     * Counts and returns the total the number of items in an array
     *
     * @param expression The argument for $size can be any expression as long as it resolves to an array.
     * @return the projection
     * @aggregation.expression $size
     */
    public static Projection size(Object expression) {
        return expression("$size", expression);
    }

    /**
     * Creates a modulo projection
     *
     * @param arg1 subtraction argument
     * @param arg2 subtraction argument
     * @return the projection
     * @aggregation.expression $mod
     */
    public static  Projection mod(Object arg1, Object arg2) {
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
