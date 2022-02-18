package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.codecs.ExpressionHelper;
import dev.morphia.aggregation.experimental.stages.Lookup;
import dev.morphia.aggregation.experimental.stages.Stage;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

public class LookupCodec extends StageCodec<Lookup> {

    public LookupCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Lookup> getEncoderClass() {
        return Lookup.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void encodeStage(BsonWriter writer, Lookup value, EncoderContext encoderContext) {
        document(writer, () -> {
            if (value.getFrom() != null) {
                writer.writeString("from", value.getFrom());
            } else {
                writer.writeString("from", getDatastore().getMapper().getEntityModel(value.getFromType()).getCollectionName());
            }

            List<Stage> pipeline = value.getPipeline();
            if (pipeline == null) {
                writer.writeString("localField", value.getLocalField());
                writer.writeString("foreignField", value.getForeignField());
            } else {
                ExpressionHelper.expression(getDatastore(), writer, "let", value.getVariables(), encoderContext);
                array(writer, "pipeline", () -> {
                    for (Stage stage : pipeline) {
                        Codec<Stage> codec = (Codec<Stage>) getCodecRegistry().get(stage.getClass());
                        codec.encode(writer, stage, encoderContext);
                    }
                });
            }
            writer.writeString("as", value.getAs());
        });
    }
}
