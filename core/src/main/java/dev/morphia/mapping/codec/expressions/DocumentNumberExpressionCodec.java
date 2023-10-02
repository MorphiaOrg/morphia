package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.DocumentNumberExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;

public class DocumentNumberExpressionCodec extends BaseExpressionCodec<DocumentNumberExpression> {
    public DocumentNumberExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, DocumentNumberExpression value, EncoderContext encoderContext) {
        document(writer, value.operation(), () -> {
        });

    }

    @Override
    public Class<DocumentNumberExpression> getEncoderClass() {
        return DocumentNumberExpression.class;
    }
}
