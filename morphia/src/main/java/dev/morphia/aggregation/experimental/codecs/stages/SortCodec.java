package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.aggregation.experimental.stages.Sort.SortType;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class SortCodec extends StageCodec<Sort> {
    public SortCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final Sort value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        for (final SortType sort : value.getSorts()) {
            writer.writeName(sort.getField());
            sort.getDirection().write(writer);
        }
        writer.writeEndDocument();
    }

    @Override
    public Class<Sort> getEncoderClass() {
        return Sort.class;
    }
}
