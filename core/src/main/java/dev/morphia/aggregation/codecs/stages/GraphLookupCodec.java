package dev.morphia.aggregation.codecs.stages;

import dev.morphia.aggregation.stages.GraphLookup;
import dev.morphia.query.filters.Filter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

public class GraphLookupCodec extends StageCodec<GraphLookup> {
    @Override
    public Class<GraphLookup> getEncoderClass() {
        return GraphLookup.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, GraphLookup value, EncoderContext encoderContext) {
        document(writer, () -> {
            if (value.getFrom() != null) {
                value(writer, "from", value.getFrom());
            } else {
                writer.writeString("from", getDatastore().getMapper().getEntityModel(value.getFromType()).getCollectionName());
            }
            expression(getDatastore(), writer, "startWith", value.getStartWith(), encoderContext);
            value(writer, "connectFromField", value.getConnectFromField());
            value(writer, "connectToField", value.getConnectToField());
            value(writer, "as", value.getAs());
            value(writer, "maxDepth", value.getMaxDepth());
            value(writer, "depthField", value.getDepthField());
            Filter[] restriction = value.getRestriction();
            if (restriction != null) {
                document(writer, "restrictSearchWithMatch", () -> {
                    for (Filter filter : restriction) {
                        filter.encode(getDatastore(), writer, encoderContext);
                    }
                });
            }
        });
    }
}
