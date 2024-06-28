package dev.morphia.mapping.codec;

import java.util.List;

import dev.morphia.MorphiaDatastore;
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
    public void encode(BsonWriter writer, UnsetOperator operator, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, operator.operator(), () -> {
                List<String> value = (List<String>) operator.value();
                value.forEach(field -> namedValue(writer, datastore, field, "", encoderContext));
            });
        });
    }

    @Override
    public Class<UnsetOperator> getEncoderClass() {
        return UnsetOperator.class;
    }
}
