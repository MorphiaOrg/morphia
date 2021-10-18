package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.AddFields;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class AddFieldsCodec extends StageCodec<AddFields> {
    public AddFieldsCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<AddFields> getEncoderClass() {
        return AddFields.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, AddFields value, EncoderContext encoderContext) {
        value.getDocument().encode(getDatastore(), writer, encoderContext);
    }
}
