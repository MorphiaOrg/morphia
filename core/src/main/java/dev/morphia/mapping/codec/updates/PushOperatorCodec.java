package dev.morphia.mapping.codec.updates;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.updates.PushOperator;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.namedValue;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class PushOperatorCodec extends BaseOperatorCodec<PushOperator> {
    public PushOperatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, PushOperator operator, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, operator.operator(), () -> {
                Object value = operator.value();
                if (value instanceof List<?> list) {
                    if (list.size() == 1 && !hasOptions(operator)) {
                        namedValue(writer, datastore, operator.field(), list.get(0), encoderContext);
                    } else {
                        document(writer, operator.field(), () -> {
                            namedValue(writer, datastore, "$each", list, encoderContext);
                            value(writer, "$position", operator.position());
                            value(writer, "$sort", operator.sort());
                            if (operator.sortDocument() != null) {
                                namedValue(writer, datastore, "$sort", operator.sortDocument(), encoderContext);
                            }
                            value(writer, "$slice", operator.slice());
                        });
                    }
                }
            });
        });

    }

    private boolean hasOptions(PushOperator operator) {
        return operator.slice() != null
                || operator.sort() != null
                || operator.position() != null
                || operator.sortDocument() != null;
    }

    @Override
    public Class<PushOperator> getEncoderClass() {
        return PushOperator.class;
    }
}
