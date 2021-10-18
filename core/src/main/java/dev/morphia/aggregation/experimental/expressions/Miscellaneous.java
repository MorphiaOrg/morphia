package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

/**
 * Defines miscellaneous operators for aggregations.
 *
 * @since 2.2
 */
public final class Miscellaneous {
    private Miscellaneous() {
    }

    /**
     * Returns a random float between 0 and 1.
     *
     * @return the filter
     * @aggregation.expression $rand
     * @since 2.2
     */
    public static Expression rand() {
        return new Expression("$rand") {
            @Override
            public void encode(Datastore datastore, BsonWriter writer, EncoderContext context) {
                document(writer, () -> {
                    document(writer, getOperation(), () -> {
                    });
                });
            }
        };
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
