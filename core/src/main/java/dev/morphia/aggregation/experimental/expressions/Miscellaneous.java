package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
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
     * Matches a random selection of input documents. The number of documents selected approximates the sample rate expressed as a
     * percentage of the total number of documents.
     *
     * @param rate the rate to check against
     * @return the filter
     * @query.filter $sampleRate
     */
    public static Filter sampleRate(double rate) {
        return new Filter("$sampleRate", null, rate) {
            @Override
            public void encode(Mapper mapper, BsonWriter writer, EncoderContext context) {
                writeNamedValue(getName(), getValue(), mapper, writer, context);
            }
        };
    }
}
