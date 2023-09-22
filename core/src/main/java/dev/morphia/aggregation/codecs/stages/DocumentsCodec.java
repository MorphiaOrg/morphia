package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Documents;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

public class DocumentsCodec extends StageCodec<Documents> {
    public DocumentsCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Documents> getEncoderClass() {
        return Documents.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Documents documents, EncoderContext encoderContext) {
        array(writer, () -> {
            documents.expressions().forEach(e -> {
                //                document(writer, () -> {
                value(getDatastore(), writer, e, encoderContext);
                //                });
            });
        });
    }
}
