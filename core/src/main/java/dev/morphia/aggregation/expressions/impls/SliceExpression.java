package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

public class SliceExpression extends Expression {
    private final Expression array;
    private final int size;
    private Integer position;

    public SliceExpression(Expression array, int size) {
        super("$slice");
        this.array = array;
        this.size = size;
    }

    public Expression array() {
        return array;
    }

    public int size() {
        return size;
    }

    public Integer position() {
        return position;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        ExpressionHelper.array(writer, operation(), () -> {
            expression(datastore, writer, array, encoderContext);
            if (position != null) {
                writer.writeInt32(position);
            }
            writer.writeInt32(size);
        });
    }

    public SliceExpression position(Integer position) {
        this.position = position;
        return this;
    }
}
