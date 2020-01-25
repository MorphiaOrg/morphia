package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class RangeExpression extends Expression {
    private final int start;
    private final int end;
    private Integer step;

    public RangeExpression(final int start, final int end) {
        super("$range");
        this.start = start;
        this.end = end;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartArray(getOperation());
        writer.writeInt32(start);
        writer.writeInt32(end);
        if (step != null) {
            writer.writeInt32(step);
        }
        writer.writeEndArray();
        writer.writeEndDocument();
    }

    public RangeExpression step(final Integer step) {
        this.step = step;
        return this;
    }
}
