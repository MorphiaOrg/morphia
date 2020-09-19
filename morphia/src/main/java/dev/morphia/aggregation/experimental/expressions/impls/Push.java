package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.AggregationException;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class Push extends Expression implements FieldHolder<Push> {
    private Expression field;
    private DocumentExpression document;

    public Push() {
        super("$push");
    }

    public Push single(Expression source) {
        if(document != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(getOperation()));
        }
        this.field = source;
        return this;
    }

    @Override
    public Push field(String name, Expression expression) {
        if(field != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(getOperation()));
        }
        if(document == null) {
            document = Expressions.of();
        }
        document.field(name, expression);

        return this;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());
        if (field != null) {
            field.encode(mapper, writer, encoderContext);
        } else if(document != null) {
            document.encode(mapper, writer, encoderContext);
        }
        writer.writeEndDocument();
    }
}
