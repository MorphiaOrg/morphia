package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.AggregationException;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.sofia.Sofia;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class Push extends Expression implements FieldHolder<Push> {
    private Expression field;
    private DocumentExpression document;

    public Push() {
        super("$push");
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeName(operation());
        if (field != null) {
            field.encode(datastore, writer, encoderContext);
        } else if (document != null) {
            document.encode(datastore, writer, encoderContext);
        }
    }

    @Override
    public Push field(String name, Expression expression) {
        if (field != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(operation()));
        }
        if (document == null) {
            document = Expressions.of();
        }
        document.field(name, expression);

        return this;
    }

    public Push single(Expression source) {
        if (document != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(operation()));
        }
        this.field = source;
        return this;
    }
}
