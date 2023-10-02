package dev.morphia.aggregation.codecs.stages;

import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.FullDocumentBeforeChange;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.ChangeStream;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.value;

public class ChangeStreamCodec extends StageCodec<ChangeStream> {
    public ChangeStreamCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<ChangeStream> getEncoderClass() {
        return ChangeStream.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, ChangeStream changeStream, EncoderContext encoderContext) {
        document(writer, () -> {
            value(writer, "allChangesForCluster", changeStream.allChangesForCluster());
            FullDocument fullDocument = changeStream.fullDocument();
            CodecRegistry registry = getDatastore().getCodecRegistry();
            if (fullDocument != FullDocument.DEFAULT) {
                value(registry, writer, "fullDocument", fullDocument.getValue(), encoderContext);
            }
            FullDocumentBeforeChange beforeChange = changeStream.fullDocumentBeforeChange();
            if (beforeChange != FullDocumentBeforeChange.DEFAULT) {
                value(registry, writer, "fullDocumentBeforeChange", beforeChange.getValue(), encoderContext);
            }
            if (changeStream.resumeAfter() != null) {
                value(registry, writer, "resumeAfter", changeStream.resumeAfter(), encoderContext);
            }
            if (changeStream.startAfter() != null) {
                value(registry, writer, "startAfter", changeStream.startAfter(), encoderContext);
            }
            if (changeStream.startAtOperationTime() != null) {
                value(registry, writer, "startAtOperationTime", changeStream.startAtOperationTime(), encoderContext);
            }
        });
    }
}
