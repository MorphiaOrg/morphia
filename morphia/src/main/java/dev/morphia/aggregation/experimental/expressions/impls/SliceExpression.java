package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeUnnamedExpression;

public class SliceExpression extends Expression {
    private final Expression array;
    private final int size;
    private Integer position;

    public SliceExpression(final Expression array, final int size) {
        super("$slice");
        this.array = array;
        this.size = size;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartArray(getOperation());
        writeUnnamedExpression(mapper, writer, array, encoderContext);
        if (position != null) {
            writer.writeInt32(position);
        }
        writer.writeInt32(size);
        writer.writeEndArray();
        writer.writeEndDocument();
    }

    public SliceExpression position(final Integer position) {
        this.position = position;
        return this;
    }
}
