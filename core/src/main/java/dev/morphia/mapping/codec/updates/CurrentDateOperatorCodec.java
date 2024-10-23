package dev.morphia.mapping.codec.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.updates.CurrentDateOperator;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

public class CurrentDateOperatorCodec extends BaseOperatorCodec<CurrentDateOperator> {
    public CurrentDateOperatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, CurrentDateOperator operator, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, operator.operator(), () -> {
                writer.writeName(operator.field());
                switch (operator.type()) {
                    case DATE -> writer.writeBoolean(true);
                    case TIMESTAMP -> document(writer, () -> {
                        writer.writeString("$type", "timestamp");
                    });
                }
            });
        });
    }

    @Override
    public Class<CurrentDateOperator> getEncoderClass() {
        return CurrentDateOperator.class;
    }
}
