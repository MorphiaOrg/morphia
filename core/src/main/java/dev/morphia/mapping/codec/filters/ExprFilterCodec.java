package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.mapping.codec.CodecHelper;
import dev.morphia.query.filters.ExprFilter;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

public class ExprFilterCodec extends BaseFilterCodec<ExprFilter> {
    public ExprFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ExprFilter filter, EncoderContext encoderContext) {
        writer.writeName(filter.getName());
        Expression value = filter.getValue();
        if (value != null) {
            Codec codec = datastore.getCodecRegistry().get(value.getClass());
            CodecHelper.encodeIfNotNull(datastore.getCodecRegistry(), writer, value, encoderContext);
        } else {
            writer.writeNull();
        }
    }

    @Override
    public Class<ExprFilter> getEncoderClass() {
        return ExprFilter.class;
    }
}
