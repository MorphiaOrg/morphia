package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;

public class RangeExpression extends Expression {
    private final int start;
    private final int end;
    private Integer step;

    public RangeExpression(int start, int end) {
        super("$range");
        this.start = start;
        this.end = end;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        array(writer, getOperation(), () -> {
            writer.writeInt32(start);
            writer.writeInt32(end);
            if (step != null) {
                writer.writeInt32(step);
            }
        });
    }

    public RangeExpression step(Integer step) {
        this.step = step;
        return this;
    }
}
