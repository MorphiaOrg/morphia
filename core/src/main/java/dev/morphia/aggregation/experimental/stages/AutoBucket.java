package dev.morphia.aggregation.experimental.stages;

import com.mongodb.client.model.BucketGranularity;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.aggregation.experimental.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;

/**
 * Categorizes incoming documents into a specific number of groups, called buckets, based on a specified expression. Bucket boundaries
 * are automatically determined in an attempt to evenly distribute the documents into the specified number of buckets.
 * <p>
 * Each bucket is represented as a document in the output. The document for each bucket contains an _id field, whose value specifies the
 * inclusive lower bound and the exclusive upper bound for the bucket, and a count field that contains the number of documents in the
 * bucket. The count field is included by default when the output is not specified.
 *
 * @aggregation.expression $bucketAuto
 */
public class AutoBucket extends Stage {
    private Expression groupBy;
    private Integer buckets;
    private DocumentExpression output;
    private BucketGranularity granularity;

    protected AutoBucket() {
        super("$bucketAuto");
    }

    /**
     * Creates a new auto bucket
     *
     * @return the new bucket
     * @since 2.2
     */
    public static AutoBucket autoBucket() {
        return new AutoBucket();
    }

    /**
     * Creates a new auto bucket
     *
     * @return the new bucket
     * @deprecated use {@link #autoBucket()}
     */
    @Deprecated(forRemoval = true)
    public static AutoBucket of() {
        return new AutoBucket();
    }

    /**
     * A positive 32-bit integer that specifies the number of buckets into which input documents are grouped.
     *
     * @param buckets the number of buckets
     * @return this
     */
    public AutoBucket buckets(Integer buckets) {
        this.buckets = buckets;
        return this;
    }

    /**
     * @return the number of buckets
     * @morphia.internal
     */
    public Integer getBuckets() {
        return buckets;
    }

    /**
     * @return the granularity
     * @morphia.internal
     */
    public BucketGranularity getGranularity() {
        return granularity;
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
    public DocumentExpression getOutput() {
        return output;
    }

    /**
     * A string that specifies the preferred number series to use to ensure that the calculated boundary edges end on preferred round
     * numbers or their powers of 10.
     * <p>
     * Available only if the all groupBy values are numeric and none of them are NaN.
     *
     * @param granularity the granularity
     * @return this
     */
    public AutoBucket granularity(BucketGranularity granularity) {
        this.granularity = granularity;
        return this;
    }

    /**
     * An expression to group documents by.
     *
     * @param groupBy the expression to use
     * @return this
     */
    public AutoBucket groupBy(Expression groupBy) {
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
    public AutoBucket outputField(String name, Expression value) {
        if (output == null) {
            output = Expressions.of();
        }
        output.field(name, value);
        return this;
    }
}
