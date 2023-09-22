package dev.morphia.aggregation.codecs.stages;

import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.FullDocumentBeforeChange;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.ChangeStream;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

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
            if (fullDocument != FullDocument.DEFAULT) {
                value(getDatastore(), writer, "fullDocument", fullDocument.getValue(), encoderContext);
            }
            FullDocumentBeforeChange beforeChange = changeStream.fullDocumentBeforeChange();
            if (beforeChange != FullDocumentBeforeChange.DEFAULT) {
                value(getDatastore(), writer, "fullDocumentBeforeChange", beforeChange.getValue(), encoderContext);
            }
            if (changeStream.resumeAfter() != null) {
                value(getDatastore(), writer, "resumeAfter", changeStream.resumeAfter(), encoderContext);
            }
            if (changeStream.startAfter() != null) {
                value(getDatastore(), writer, "startAfter", changeStream.startAfter(), encoderContext);
            }
            if (changeStream.startAtOperationTime() != null) {
                value(getDatastore(), writer, "startAtOperationTime", changeStream.startAtOperationTime(), encoderContext);
            }
        });
    }
}
