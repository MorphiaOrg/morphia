package dev.morphia.aggregation.experimental.codecs.stages;

import com.mongodb.client.MongoCollection;
import dev.morphia.aggregation.experimental.stages.Lookup;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class LookupCodec extends StageCodec<Lookup> {

    public LookupCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final Lookup value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        if(value.getFrom() != null) {
            writeNamedValue(writer, "from", value.getFrom(), encoderContext);
        } else {
            MongoCollection collection = getMapper().getCollection(value.getFromType());
            writer.writeString("from", collection.getNamespace().getCollectionName());
        }

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
