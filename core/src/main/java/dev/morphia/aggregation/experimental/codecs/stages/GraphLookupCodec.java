package dev.morphia.aggregation.experimental.codecs.stages;

import com.mongodb.client.MongoCollection;
import dev.morphia.aggregation.experimental.stages.GraphLookup;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class GraphLookupCodec extends StageCodec<GraphLookup> {
    public GraphLookupCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<GraphLookup> getEncoderClass() {
        return GraphLookup.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void encodeStage(BsonWriter writer, GraphLookup value, EncoderContext encoderContext) {
        document(writer, () -> {
            if (value.getFrom() != null) {
                value(getMapper(), writer, "from", value.getFrom(), encoderContext);
            } else {
                MongoCollection collection = getMapper().getCollection(value.getFromType());
                writer.writeString("from", collection.getNamespace().getCollectionName());
            }
            expression(getMapper(), writer, "startWith", value.getStartWith(), encoderContext);
            value(getMapper(), writer, "connectFromField", value.getConnectFromField(), encoderContext);
            value(getMapper(), writer, "connectToField", value.getConnectToField(), encoderContext);
            value(getMapper(), writer, "as", value.getAs(), encoderContext);
            value(getMapper(), writer, "maxDepth", value.getMaxDepth(), encoderContext);
            value(getMapper(), writer, "depthField", value.getDepthField(), encoderContext);
            Filter[] restriction = value.getRestriction();
            if (restriction != null) {
                document(writer, "restrictSearchWithMatch", () -> {
                    for (Filter filter : restriction) {
                        filter.encode(getMapper(), writer, encoderContext);
                    }
                });
            }
        });
    }
}
