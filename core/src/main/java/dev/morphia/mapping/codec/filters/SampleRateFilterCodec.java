package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.SampleRateFilter;
import dev.morphia.mapping.codec.CodecHelper;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class SampleRateFilterCodec extends BaseFilterCodec<SampleRateFilter> {
    public SampleRateFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, SampleRateFilter filter, EncoderContext encoderContext) {
        CodecHelper.namedValue(writer, datastore, filter.getName(), filter.getValue(), encoderContext);
    }

    @Override
    public Class<SampleRateFilter> getEncoderClass() {
        return SampleRateFilter.class;
    }
}
