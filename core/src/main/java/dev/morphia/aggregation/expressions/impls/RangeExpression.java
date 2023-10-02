package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;
import dev.morphia.MorphiaDatastore;

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

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    @Nullable
    public Integer step() {
        return step;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        array(writer, operation(), () -> {
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
