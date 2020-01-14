package dev.morphia.aggregation.experimental.codecs.stages;

import com.mongodb.client.MongoCollection;
import dev.morphia.aggregation.experimental.stages.GraphLookup;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeNamedExpression;

public class GraphLookupCodec extends StageCodec<GraphLookup> {
    public GraphLookupCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<GraphLookup> getEncoderClass() {
        return GraphLookup.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void encodeStage(final BsonWriter writer, final GraphLookup value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        if(value.getFrom() != null) {
            writeNamedValue(writer, "from", value.getFrom(), encoderContext);
        } else {
            MongoCollection collection = getMapper().getCollection(value.getFromType());
            writer.writeString("from", collection.getNamespace().getCollectionName());
        }
        writeNamedExpression(getMapper(), writer, "startWith", value.getStartWith(), encoderContext);
        writeNamedValue(writer, "connectFromField", value.getConnectFromField(), encoderContext);
        writeNamedValue(writer, "connectToField", value.getConnectToField(), encoderContext);
        writeNamedValue(writer, "as", value.getAs(), encoderContext);
        writeNamedValue(writer, "maxDepth", value.getMaxDepth(), encoderContext);
        writeNamedValue(writer, "depthField", value.getDepthField(), encoderContext);
        writeNamedValue(writer, "restrictSearchWithMatch", value.getRestriction(), encoderContext);

        writer.writeEndDocument();
    }
}
