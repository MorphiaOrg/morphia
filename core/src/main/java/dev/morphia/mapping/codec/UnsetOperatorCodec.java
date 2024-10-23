package dev.morphia.mapping.codec;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.updates.BaseOperatorCodec;
import dev.morphia.query.updates.UnsetOperator;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.namedValue;

public class UnsetOperatorCodec extends BaseOperatorCodec<UnsetOperator> {
    public UnsetOperatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode(BsonWriter writer, UnsetOperator operator, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, operator.operator(), () -> {
                for (String field : ((List<String>) operator.value())) {
                    var target = new PathTarget(datastore.getMapper(), operator.model(), field, operator.validate()).translatedPath();
                    namedValue(writer, datastore, target, "", encoderContext);
                }
            });
        });
    }

    @Override
    public Class<UnsetOperator> getEncoderClass() {
        return UnsetOperator.class;
    }
}
