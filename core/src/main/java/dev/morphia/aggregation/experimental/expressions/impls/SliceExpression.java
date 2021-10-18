package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.codecs.ExpressionHelper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;

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
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> {
            ExpressionHelper.array(writer, getOperation(), () -> {
                expression(datastore, writer, array, encoderContext);
                if (position != null) {
                    writer.writeInt32(position);
                }
                writer.writeInt32(size);
            });
        });
    }

    public SliceExpression position(Integer position) {
        this.position = position;
        return this;
    }
}
