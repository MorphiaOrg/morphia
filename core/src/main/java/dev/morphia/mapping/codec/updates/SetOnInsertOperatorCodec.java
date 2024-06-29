package dev.morphia.mapping.codec.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.internal.PathTarget;
import dev.morphia.query.updates.SetOnInsertOperator;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.namedValue;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class SetOnInsertOperatorCodec extends BaseOperatorCodec<SetOnInsertOperator> {
    public SetOnInsertOperatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, SetOnInsertOperator operator, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, operator.operator(), () -> {
                var model = operator.model();
                var mapper = datastore.getMapper();
                operator.insertValues().forEach((key, value) -> {
                    PathTarget keyTarget = new PathTarget(mapper, model, key, operator.validate());
                    namedValue(writer, datastore, keyTarget.translatedPath(), value, encoderContext);
                });
            });
        });
    }

    @Override
    public Class<SetOnInsertOperator> getEncoderClass() {
        return SetOnInsertOperator.class;
    }
}
