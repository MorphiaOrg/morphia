package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.ValueExpression;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

/**
 * Defines miscellaneous operators for aggregations.
 *
 * @since 2.2
 */
public final class Miscellaneous {
    private Miscellaneous() {
    }

    /**
     * Returns the value of a specified field from a document. If you don't specify an object, $getField returns the value of the field
     * from $$CURRENT.
     *
     * @param field the field name
     * @return the new expression
     * @aggregation.expression $getField
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static Expression getField(String field) {
        return getField(new ValueExpression(field));
    }

    /**
     * Returns the value of a specified field from a document. If you don't specify an object, $getField returns the value of the field
     * from $$CURRENT.
     *
     * @param field the expression yielding the field name
     * @return the new expression
     * @aggregation.expression $getField
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static Expression getField(Expression field) {
        return new Expression("$getField", field);
    }

    /**
     * Returns a random float between 0 and 1.
     *
     * @return the filter
     * @aggregation.expression $rand
     * @since 2.2
     */
    public static Expression rand() {
        return new Expression("$rand", new DocumentExpression());
    }

    /**
     * Matches a random selection of input documents. The number of documents selected approximates the sample rate expressed as a
     * percentage of the total number of documents.
     *
     * @param rate the rate to check against
     * @return the filter
     * @aggregation.expression $sampleRate
     * @since 2.2
     */
    public static Filter sampleRate(double rate) {
        return new Filter("$sampleRate", null, rate) {
            @Override
            public void encode(Datastore datastore, BsonWriter writer, EncoderContext context) {
                writeNamedValue(getName(), getValue(), datastore, writer, context);
            }
        };
    }
}
