package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.RangeExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.array;

public class RangeExpressionCodec extends BaseExpressionCodec<RangeExpression> {
    public RangeExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, RangeExpression range, EncoderContext encoderContext) {
        array(writer, range.operation(), () -> {
            writer.writeInt32(range.start());
            writer.writeInt32(range.end());
            Integer step = range.step();
            if (step != null) {
                writer.writeInt32(step);
            }
        });

    }

    @Override
    public Class<RangeExpression> getEncoderClass() {
        return RangeExpression.class;
    }
}
