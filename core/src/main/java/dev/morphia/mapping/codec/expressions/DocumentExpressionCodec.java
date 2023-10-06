package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Fields;
import dev.morphia.aggregation.expressions.impls.PipelineField;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class DocumentExpressionCodec extends BaseExpressionCodec<DocumentExpression> {
    public DocumentExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void encode(BsonWriter writer, DocumentExpression value, EncoderContext encoderContext) {
        document(writer, () -> {
            Fields fields = value.fields();
            if (fields != null) {
                for (PipelineField field : fields.fields()) {
                    encodeIfNotNull(datastore.getCodecRegistry(), writer, field.name(), field.value(), encoderContext);
                }
            }
        });
    }

    @Override
    public Class<DocumentExpression> getEncoderClass() {
        return DocumentExpression.class;
    }
}
