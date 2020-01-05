package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.CurrentOp;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class CurrentOpCodec extends StageCodec<CurrentOp> {
    public CurrentOpCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<CurrentOp> getEncoderClass() {
        return CurrentOp.class;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final CurrentOp value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writeBoolean(writer, "allUsers", value.isAllUsers(), encoderContext);
        writeBoolean(writer, "idleConnections", value.isIdleConnections(), encoderContext);
        writeBoolean(writer, "idleCursors", value.isIdleCursors(), encoderContext);
        writeBoolean(writer, "idleSessions", value.isIdleSessions(), encoderContext);
        writeBoolean(writer, "localOps", value.isLocalOps(), encoderContext);
        writer.writeEndDocument();
    }

    private void writeBoolean(final BsonWriter writer, String name, boolean value, EncoderContext context) {
        if(value) {
            writeNamedValue(writer, name, value, context);
        }
    }

}
