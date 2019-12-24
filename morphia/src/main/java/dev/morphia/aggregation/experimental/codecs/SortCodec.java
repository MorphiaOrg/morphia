package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.aggregation.experimental.stages.Sort.Direction;
import dev.morphia.aggregation.experimental.stages.Sort.SortType;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class SortCodec implements Codec<Sort> {
    @Override
    public Sort decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(final BsonWriter writer, final Sort value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument("$sort");
        for (final SortType sort : value.getSorts()) {
            writer.writeName(sort.getField());
            sort.getDirection().write(writer);
        }
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    @Override
    public Class<Sort> getEncoderClass() {
        return Sort.class;
    }
}
