package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeUnnamedExpression;

public class SliceExpression extends Expression {
    private final Expression array;
    private final int size;
    private Integer position;

    public SliceExpression(Expression array, int size) {
        super("$slice");
        this.array = array;
        this.size = size;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
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

    public SliceExpression position(Integer position) {
        this.position = position;
        return this;
    }
}
