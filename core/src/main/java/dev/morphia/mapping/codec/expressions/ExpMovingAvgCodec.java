package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.ExpMovingAvg;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class ExpMovingAvgCodec extends BaseExpressionCodec<ExpMovingAvg> {
    public ExpMovingAvgCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ExpMovingAvg avg, EncoderContext encoderContext) {
        document(writer, avg.operation(), () -> {
            writer.writeName("input");
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, avg.input(), encoderContext);
            if (avg.n() != null) {
                value(writer, "N", avg.n());
            } else {
                value(writer, "alpha", avg.alpha());
            }
        });

    }

    @Override
    public Class<ExpMovingAvg> getEncoderClass() {
        return ExpMovingAvg.class;
    }
}
