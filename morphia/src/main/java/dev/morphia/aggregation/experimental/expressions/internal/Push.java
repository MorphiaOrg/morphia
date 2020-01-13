package dev.morphia.aggregation.experimental.expressions.internal;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.FieldHolder;
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

    public Push single(final Expression source) {
        if(document != null) {
            throw new IllegalStateException(Sofia.mixedModesNotAllowed(getOperation()));
        }
        this.field = source;
        return this;
    }

    @Override
    public Push field(final String name, final Expression expression) {
        if(field != null) {
            throw new IllegalStateException(Sofia.mixedModesNotAllowed(getOperation()));
        }
        if(document == null) {
            document = Expression.of();
        }
        document.field(name, expression);

        return this;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(operation);
        if (field != null) {
            field.encode(mapper, writer, encoderContext);
        } else if(document != null) {
            document.encode(mapper, writer, encoderContext);
        }
        writer.writeEndDocument();
    }
}
