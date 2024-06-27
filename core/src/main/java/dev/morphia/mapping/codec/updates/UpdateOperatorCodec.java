package dev.morphia.mapping.codec.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.query.updates.UpdateOperator;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.namedValue;
import static dev.morphia.mapping.codec.CodecHelper.unnamedValue;

public class UpdateOperatorCodec extends BaseOperatorCodec<UpdateOperator> {
    public UpdateOperatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, UpdateOperator operator, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, operator.operator(), () -> {
                Object value = operator.value();
                if (value instanceof Expression) {
                    document(writer, operator.field(), () -> {
                        unnamedValue(writer, datastore, value, encoderContext);
                    });
                } else {
                    namedValue(writer, datastore, operator.field(), value, encoderContext);
                }
            });
        });
    }

    @Override
    public Class<UpdateOperator> getEncoderClass() {
        return UpdateOperator.class;
    }
}
