package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.GraphLookup;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class GraphLookupCodec extends StageCodec<GraphLookup> {
    public GraphLookupCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<GraphLookup> getEncoderClass() {
        return GraphLookup.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, GraphLookup value, EncoderContext encoderContext) {
        document(writer, () -> {
            if (value.getFrom() != null) {
                value(getDatastore(), writer, "from", value.getFrom(), encoderContext);
            } else {
                writer.writeString("from", getDatastore().getMapper().getEntityModel(value.getFromType()).getCollectionName());
            }
            expression(getDatastore(), writer, "startWith", value.getStartWith(), encoderContext);
            value(getDatastore(), writer, "connectFromField", value.getConnectFromField(), encoderContext);
            value(getDatastore(), writer, "connectToField", value.getConnectToField(), encoderContext);
            value(getDatastore(), writer, "as", value.getAs(), encoderContext);
            value(getDatastore(), writer, "maxDepth", value.getMaxDepth(), encoderContext);
            value(getDatastore(), writer, "depthField", value.getDepthField(), encoderContext);
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
