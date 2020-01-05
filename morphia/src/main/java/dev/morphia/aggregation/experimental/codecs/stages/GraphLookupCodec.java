package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.GraphLookup;
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
    protected void encodeStage(final BsonWriter writer, final GraphLookup value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writeNamedValue(writer, "from", value.getFrom(), encoderContext);
        writeNamedExpression(getMapper(), writer, "startWith", value.getStartWith(), encoderContext);
        writeNamedValue(writer, "connectFromField", value.getConnectFromField(), encoderContext);
        writeNamedValue(writer, "connectToField", value.getConnectToField(), encoderContext);
        writeNamedValue(writer, "as", value.getAs(), encoderContext);
        writeNamedValue(writer, "maxDepth", value.getMaxDepth(), encoderContext);
        writeNamedValue(writer, "depthField", value.getDepthField(), encoderContext);
        writeNamedValue(writer, "restrictSearchWithMatch", value.getRestrictWithMatch(), encoderContext);

        writer.writeEndDocument();
    }
}
