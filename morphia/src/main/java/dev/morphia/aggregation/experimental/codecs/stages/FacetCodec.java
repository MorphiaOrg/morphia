package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Facet;
import dev.morphia.aggregation.experimental.stages.Stage;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;
import java.util.Map.Entry;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class FacetCodec extends StageCodec<Facet> {
    public FacetCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<Facet> getEncoderClass() {
        return Facet.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Facet value, EncoderContext encoderContext) {
        document(writer, () -> {
            for (Entry<String, List<Stage>> entry : value.getFields().entrySet()) {
                value(getMapper(), writer, entry.getKey(), entry.getValue(), encoderContext);
            }
        });
    }
}
