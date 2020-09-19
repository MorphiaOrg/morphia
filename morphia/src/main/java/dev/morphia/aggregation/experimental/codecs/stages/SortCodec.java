package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.aggregation.experimental.stages.Sort.SortType;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class SortCodec extends StageCodec<Sort> {
    public SortCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    protected void encodeStage(BsonWriter writer, Sort value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        for (SortType sort : value.getSorts()) {
            writer.writeName(sort.getField());
            sort.getDirection().encode(writer);
        }
        writer.writeEndDocument();
    }

    @Override
    public Class<Sort> getEncoderClass() {
        return Sort.class;
    }
}
