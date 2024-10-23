package dev.morphia.mapping.codec.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.updates.PopOperator;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.namedValue;

public class PopOperatorCodec extends BaseOperatorCodec<PopOperator> {
    public PopOperatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, PopOperator operator, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, operator.operator(), () -> {
                namedValue(writer, datastore, operator.field(), operator.value(), encoderContext);
            });
        });

    }

    @Override
    public Class<PopOperator> getEncoderClass() {
        return PopOperator.class;
    }
}
