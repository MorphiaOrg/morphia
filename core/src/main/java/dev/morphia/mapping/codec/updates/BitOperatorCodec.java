package dev.morphia.mapping.codec.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.updates.BitOperator;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.namedValue;

public class BitOperatorCodec extends BaseOperatorCodec<BitOperator> {
    public BitOperatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, BitOperator operator, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, operator.operator(), () -> {
                document(writer, operator.field(), () -> {
                    namedValue(writer, datastore, operator.operation(), operator.value(), encoderContext);
                });
            });
        });

    }

    @Override
    public Class<BitOperator> getEncoderClass() {
        return BitOperator.class;
    }
}
