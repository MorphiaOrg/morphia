package dev.morphia.aggregation.stages;

import com.mongodb.lang.Nullable;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;

/**
 * Categorizes incoming documents into groups, called buckets, based on a specified expression and bucket boundaries.
 * <p>
 * Each bucket is represented as a document in the output. The document for each bucket contains an _id field, whose value specifies the
 * inclusive lower bound of the bucket and a count field that contains the number of documents in the bucket. The count field is included
 * by default when the output is not specified.
 * <p>
 * $bucket only produces output documents for buckets that contain at least one input document.
 *
 * @aggregation.expression $bucket
 */
public class Bucket extends Stage {
    private Expression groupBy;
    private ExpressionList boundaries;
    private Object defaultValue;
    private DocumentExpression output;

    protected Bucket() {
        super("$bucket");
    }

    /**
     * Creates a new bucket stage
     *
     * @return the new stage
     * @since 2.2
     */
    public static Bucket bucket() {
        return new Bucket();
    }

    /**
     * Creates a new bucket stage
     *
     * @return the new stage
     * @deprecated use {@link #bucket()}
     */
    @Deprecated(forRemoval = true)
    public static Bucket of() {
        return new Bucket();
    }

    /**
     * An array of values based on the groupBy expression that specify the boundaries for each bucket. Each adjacent pair of values acts
     * as the inclusive lower boundary and the exclusive upper boundary for the bucket. You must specify at least two boundaries.
     * <p>
     * The specified values must be in ascending order and all of the same type.
     *
     * @param boundaries the boundaries
     * @return this
     */
    public Bucket boundaries(Expression... boundaries) {
        this.boundaries = new ExpressionList(boundaries);
        return this;
    }

    /**
     * Optional.  A literal that specifies the _id of an additional bucket that contains all documents whose groupBy expression result does
     * not fall into a bucket specified by boundaries.
     * <p>
     * If unspecified, each input document must resolve the groupBy expression to a value within one of the bucket ranges specified by
     * boundaries or the operation throws an error.
     *
     * @param defaultValue the default value
     * @return this
     */
    public Bucket defaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * @return the boundaries
     * @morphia.internal
     */
    public ExpressionList getBoundaries() {
        return boundaries;
    }

    /**
     * @return the default value
     * @morphia.internal
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the group by expression
     * @morphia.internal
     */
    public Expression getGroupBy() {
        return groupBy;
    }

    /**
     * @return the output document
     * @morphia.internal
     */
    @Nullable
    public DocumentExpression getOutput() {
        return output;
    }

    /**
     * An expression to group documents by. To specify a field path, prefix the field name with a dollar sign $ and enclose it in quotes.
     * <p>
     * Unless $bucket includes a default specification, each input document must resolve the groupBy field path or expression to a value
     * that falls within one of the ranges specified by the boundaries.
     *
     * @param groupBy the grouping expression
     * @return this
     */
    public Bucket groupBy(Expression groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    /**
     * Adds a field to the document that specifies the fields to include in the output documents in addition to the _id field. To specify
     * the field to include, you must use accumulator expressions.
     *
     * @param name  the new field name
     * @param value the value expression
     * @return this
     */
    public Bucket outputField(String name, Expression value) {
        if (output == null) {
            output = Expressions.of();
        }
        output.field(name, value);
        return this;
    }
}
