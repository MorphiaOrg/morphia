package dev.morphia.aggregation.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.stages.Facet;
import dev.morphia.aggregation.stages.Stage;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.List;
import java.util.Map.Entry;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

public class FacetCodec extends StageCodec<Facet> {
    public FacetCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Facet> getEncoderClass() {
        return Facet.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Facet value, EncoderContext encoderContext) {
        document(writer, () -> {
            for (Entry<String, List<Stage>> entry : value.getFields().entrySet()) {
                array(writer, entry.getKey(), () -> {
                    List<Stage> stages = entry.getValue();
                    for (Stage stage : stages) {
                        Codec codec = getCodecRegistry().get(stage.getClass());
                        codec.encode(writer, stage, encoderContext);
                    }
                });
            }
        });
    }
}
