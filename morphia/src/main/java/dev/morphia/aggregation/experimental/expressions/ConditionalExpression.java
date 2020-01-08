package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.*;

public class ConditionalExpression extends Expression {
    protected ConditionalExpression(final String operation) {
        super(operation);
    }

    /**
     * Evaluates an expression and returns the value of the expression if the expression evaluates to a non-null value. If the
     * expression evaluates to a null value, including instances of undefined values or missing fields, returns the value of the
     * replacement expression.
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/ifNull $ifNull
     */
    public static IfNull ifNull() {
        return new IfNull();
    }

    public static class IfNull extends Expression implements FieldHolder<IfNull> {
        private Expression target;
        private Expression replacement;
        private DocumentExpression document;

        protected IfNull() {
            super("$ifNull");
        }

        @Override
        public IfNull field(final String name, final Expression expression) {
            if(replacement != null) {
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
            writer.writeStartArray();
            writeUnnamedExpression(mapper, writer, target, encoderContext);
            writeUnnamedExpression(mapper, writer, replacement, encoderContext);
            writeUnnamedExpression(mapper, writer, document, encoderContext);
            writer.writeEndArray();
            writer.writeEndDocument();
        }

        public DocumentExpression getDocument() {
            return document;
        }

        public Expression getReplacement() {
            return replacement;
        }

        public Expression getTarget() {
            return target;
        }

        public IfNull replacement(final Expression replacement) {
            this.replacement = replacement;
            return this;
        }

        public IfNull target(final Expression target) {
            this.target = target;
            return this;
        }
    }
}
