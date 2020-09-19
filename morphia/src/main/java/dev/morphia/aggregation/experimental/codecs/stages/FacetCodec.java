package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Facet;
import dev.morphia.aggregation.experimental.stages.Stage;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;
import java.util.Map.Entry;

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
        writer.writeStartDocument();
        for (Entry<String, List<Stage>> entry : value.getFields().entrySet()) {
          writeNamedValue(writer, entry.getKey(), entry.getValue(), encoderContext);
        }
        writer.writeEndDocument();
    }
}
