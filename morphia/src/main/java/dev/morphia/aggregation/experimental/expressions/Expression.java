package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

public abstract class Expression {
    protected final String operation;
    protected final String name;
    protected final Object value;

    protected Expression(final String operation) {
        this.operation = operation;
        this.name = null;
        this.value = null;
    }

    protected Expression(final String operation, final String name, final Object value) {
        this.operation = operation;
        this.name = name;
        this.value = value;
    }

    public Expression(final String operation, final String name) {
        this.operation = operation;
        this.name = name;
        this.value = null;
    }

    public static Expression field(final String name) {
        return new Literal(name.startsWith("$") ? name : "$" + name);
    }

    public static Expression literal(final Object value) {
        return new Literal(value);
    }

    public static PushExpression push(final String name) {
        return new PushExpression(name);
    }

    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument(name);
        writer.writeName(operation);
        Codec codec = mapper.getCodecRegistry().get(value.getClass());
        encoderContext.encodeWithChildContext(codec, writer, value);
        writer.writeEndDocument();
    }

    public String getOperation() {
        return operation;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    protected void writeUnnamedExpression(final Mapper mapper, final BsonWriter writer, final Expression operand,
                                          final EncoderContext encoderContext) {
        Codec codec = mapper.getCodecRegistry().get(operand.getClass());
        encoderContext.encodeWithChildContext(codec, writer, operand);
    }
}
