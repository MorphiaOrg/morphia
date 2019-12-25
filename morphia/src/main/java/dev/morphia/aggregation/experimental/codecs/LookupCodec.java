package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.Lookup;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class LookupCodec extends StageCodec<Lookup> {
    private Mapper mapper;

    public LookupCodec(final Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final Lookup value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeString("from", mapper.getMappedClass(value.getSource()).getCollectionName());
        writer.writeString("localField", value.getLocalField());
        writer.writeString("foreignField", value.getForeignField());
        writer.writeString("as", value.getAs());
        writer.writeEndDocument();
    }

    @Override
    public Class<Lookup> getEncoderClass() {
        return Lookup.class;
    }
}
