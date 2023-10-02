package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.GraphLookup;
import dev.morphia.query.filters.Filter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.value;

public class GraphLookupCodec extends StageCodec<GraphLookup> {
    public GraphLookupCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<GraphLookup> getEncoderClass() {
        return GraphLookup.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, GraphLookup value, EncoderContext encoderContext) {
        document(writer, () -> {
            CodecRegistry registry = getCodecRegistry();
            if (value.getFrom() != null) {
                value(writer, "from", value.getFrom());
            } else {
                writer.writeString("from", getDatastore().getMapper().getEntityModel(value.getFromType()).getCollectionName());
            }
            encodeIfNotNull(registry, writer, "startWith", value.getStartWith(), encoderContext);
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
