package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.StringJoiner;

public abstract class Expression {
    protected final String operation;
    protected final Object value;

    protected Expression(final String operation) {
        this.operation = operation;
        this.value = null;
    }

    protected Expression(final String operation, final Object value) {
        this.operation = operation;
        this.value = value;
    }

    public static Expression field(final String name) {
        return new Literal(name.startsWith("$") ? name : "$" + name);
    }

    /**
     * Returns a value without parsing. Use for values that the aggregation pipeline may interpret as an expression.
     *
     * @return the new expression
     *
     * @mongodb.driver.manual reference/operator/aggregation/literal $literal
     */
    public static Expression literal(final Object value) {
        return new Literal(value);
    }

    /**
     * Returns an array of all values that result from applying an expression to each document in a group of documents that share the
     * same group by key.
     *
     * $push is only available in the $group stage.
     *
     * @return the new expression
     *
     * @mongodb.driver.manual reference/operator/aggregation/push $push
     */
    public static Push push() {
        return new Push();
    }

    public static DocumentExpression of() {
        return new DocumentExpression();
    }

    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(operation);
        Codec codec = mapper.getCodecRegistry().get(value.getClass());
        encoderContext.encodeWithChildContext(codec, writer, value);
        writer.writeEndDocument();
    }

    public String getOperation() {
        return operation;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Expression.class.getSimpleName() + "[", "]")
                   .add("operation='" + operation + "'")
                   .toString();
    }

    public static class DocumentExpression extends Expression implements FieldHolder<DocumentExpression> {
        private Fields<DocumentExpression> fields = Fields.on(this);
        private DocumentExpression() {
            super(null);
        }

        @Override
        public DocumentExpression field(final String name, final Expression expression) {
            return fields.add(name, expression);
        }

        public void encode(final String name, final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
            writer.writeStartDocument(name);
            fields.encode(mapper, writer, encoderContext);
            writer.writeEndDocument();
        }

        @Override
        public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
            writer.writeStartDocument();
            fields.encode(mapper, writer, encoderContext);
            writer.writeEndDocument();
        }
    }

}
