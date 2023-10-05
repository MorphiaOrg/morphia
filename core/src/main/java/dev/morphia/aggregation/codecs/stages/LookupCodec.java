package dev.morphia.aggregation.codecs.stages;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Lookup;
import dev.morphia.aggregation.stages.Stage;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class LookupCodec extends StageCodec<Lookup> {
    public LookupCodec(MorphiaDatastore datastore) {
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
            } else if (value.getFromType() != null) {
                writer.writeString("from", getDatastore().getMapper().getEntityModel(value.getFromType()).getCollectionName());
            }

            if (value.getLocalField() != null) {
                writer.writeString("localField", value.getLocalField());
            }
            if (value.getForeignField() != null) {
                writer.writeString("foreignField", value.getForeignField());
            }
            writer.writeString("as", value.getAs());
            List<Stage> pipeline = value.getPipeline();
            if (pipeline != null) {
                encodeIfNotNull(getCodecRegistry(), writer, "let", value.getVariables(), encoderContext);
                array(writer, "pipeline", () -> {
                    for (Stage stage : pipeline) {
                        Codec<Stage> codec = (Codec<Stage>) getCodecRegistry().get(stage.getClass());
                        codec.encode(writer, stage, encoderContext);
                    }
                });
            }
        });
    }
}
