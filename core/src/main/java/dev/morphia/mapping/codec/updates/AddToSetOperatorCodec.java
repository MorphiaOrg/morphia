package dev.morphia.mapping.codec.updates;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.updates.AddToSetOperator;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.namedValue;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class AddToSetOperatorCodec extends BaseOperatorCodec<AddToSetOperator> {
    public AddToSetOperatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, AddToSetOperator operator, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, operator.operator(), () -> {
                Object value = operator.value();
                if (value instanceof List<?> list && list.size() > 1) {
                    document(writer, operator.field(), () -> {
                        namedValue(writer, datastore, "$each", list, encoderContext);
                    });
                } else {
                    namedValue(writer, datastore, operator.field(), value, encoderContext);
                }

            });
        });

    }

    @Override
    public Class<AddToSetOperator> getEncoderClass() {
        return AddToSetOperator.class;
    }
}
