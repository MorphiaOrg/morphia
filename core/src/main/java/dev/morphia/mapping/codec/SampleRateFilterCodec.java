package dev.morphia.mapping.codec;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.SampleRateFilter;
import dev.morphia.mapping.codec.filters.BaseFilterCodec;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class SampleRateFilterCodec extends BaseFilterCodec<SampleRateFilter> {
    public SampleRateFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, SampleRateFilter filter, EncoderContext encoderContext) {
        writeNamedValue(filter.getName(), filter.getValue(), datastore, writer, encoderContext);
    }

    @Override
    public Class<SampleRateFilter> getEncoderClass() {
        return SampleRateFilter.class;
    }
}
