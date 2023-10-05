package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.SliceExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class SliceExpressionCodec extends BaseExpressionCodec<SliceExpression> {
    public SliceExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, SliceExpression slice, EncoderContext encoderContext) {
        array(writer, slice.operation(), () -> {
            encodeIfNotNull(datastore.getCodecRegistry(), writer, slice.array(), encoderContext);
            Integer position = slice.position();
            if (position != null) {
                writer.writeInt32(position);
            }
            writer.writeInt32(slice.size());
        });

    }

    @Override
    public Class<SliceExpression> getEncoderClass() {
        return SliceExpression.class;
    }
}
