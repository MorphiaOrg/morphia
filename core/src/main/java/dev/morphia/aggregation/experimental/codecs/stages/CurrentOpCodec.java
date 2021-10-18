package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.CurrentOp;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class CurrentOpCodec extends StageCodec<CurrentOp> {
    public CurrentOpCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<CurrentOp> getEncoderClass() {
        return CurrentOp.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, CurrentOp value, EncoderContext encoderContext) {
        document(writer, () -> {
            writeBoolean(writer, "allUsers", value.isAllUsers(), encoderContext);
            writeBoolean(writer, "idleConnections", value.isIdleConnections(), encoderContext);
            writeBoolean(writer, "idleCursors", value.isIdleCursors(), encoderContext);
            writeBoolean(writer, "idleSessions", value.isIdleSessions(), encoderContext);
            writeBoolean(writer, "localOps", value.isLocalOps(), encoderContext);
        });
    }

    private void writeBoolean(BsonWriter writer, String name, boolean value, EncoderContext context) {
        if (value) {
            value(getDatastore(), writer, name, value, context);
        }
    }

}
