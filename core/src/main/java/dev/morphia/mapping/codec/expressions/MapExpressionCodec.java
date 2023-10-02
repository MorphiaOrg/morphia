package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;
import dev.morphia.aggregation.expressions.impls.MapExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class MapExpressionCodec extends BaseExpressionCodec<MapExpression> {
    public MapExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, MapExpression math, EncoderContext encoderContext) {
        document(writer, math.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "input", math.input(), encoderContext);
            encodeIfNotNull(registry, writer, "in", math.in(), encoderContext);
            String as = math.as();
            if (as != null) {
                writer.writeString("as", as);
            }
        });

    }

    @Override
    public Class<MapExpression> getEncoderClass() {
        return MapExpression.class;
    }
}
