package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;
import dev.morphia.aggregation.expressions.impls.DateToParts;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class DateToPartsCodec extends BaseExpressionCodec<DateToParts> {
    public DateToPartsCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, DateToParts value, EncoderContext encoderContext) {
        document(writer, value.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "date", value.date(), encoderContext);
            encodeIfNotNull(registry, writer, "timezone", value.timeZone(), encoderContext);
            encodeIfNotNull(registry, writer, "iso8601", value.iso8601(), encoderContext);
        });

    }

    @Override
    public Class<DateToParts> getEncoderClass() {
        return DateToParts.class;
    }
}
