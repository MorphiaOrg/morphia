package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class Push extends Expression {
    private Expression field;
    private Fields<Push> fields;

    protected Push() {
        super("$push");
    }

    public Push single(final Expression source) {
        if(fields != null) {
            throw new IllegalStateException(Sofia.mixedPushModes());
        }
        this.field = source;
        return this;
    }

    public Push field(final String name, final Expression expression) {
        if(field != null) {
            throw new IllegalStateException(Sofia.mixedPushModes());
        }
        if(fields == null) {
            fields = Expression.fields(this);
        }
        return fields.add(name, expression);
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(operation);
        if (field != null) {
            field.encode(mapper, writer, encoderContext);
        } else if(fields.size() != 0) {
            writer.writeStartDocument();
            fields.encode(mapper, writer, encoderContext);
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
    }
}
