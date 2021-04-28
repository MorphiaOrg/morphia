package dev.morphia.aggregation.experimental.codecs.stages;

import com.mongodb.client.MongoCollection;
import dev.morphia.aggregation.experimental.stages.Lookup;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class LookupCodec extends StageCodec<Lookup> {

    public LookupCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<Lookup> getEncoderClass() {
        return Lookup.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Lookup value, EncoderContext encoderContext) {
        document(writer, () -> {
            if (value.getFrom() != null) {
                value(getMapper(), writer, "from", value.getFrom(), encoderContext);
            } else {
                MongoCollection collection = getMapper().getCollection(value.getFromType());
                writer.writeString("from", collection.getNamespace().getCollectionName());
            }

            if (value.getPipeline().size() == 0) {
                writer.writeString("localField", value.getLocalField());
                writer.writeString("foreignField", value.getForeignField());
            } else {
                value(getMapper(), writer, "let", value.getVariables(), encoderContext);
                value(getMapper(), writer, "pipeline", value.getPipeline(), encoderContext);
            }
            writer.writeString("as", value.getAs());
        });
    }
}
