package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.stages.AddFields;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

public class AddFieldsCodec extends StageCodec<AddFields> {

    public AddFieldsCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<AddFields> getEncoderClass() {
        return AddFields.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, AddFields value, EncoderContext encoderContext) {
        Codec<DocumentExpression> codec = getCodecRegistry().get(DocumentExpression.class);
//        document(writer, () -> {
            codec.encode(writer, value.getDocument(), encoderContext);
//        });
//        value.getDocument().encode(getDatastore(), writer, encoderContext);
    }
}
