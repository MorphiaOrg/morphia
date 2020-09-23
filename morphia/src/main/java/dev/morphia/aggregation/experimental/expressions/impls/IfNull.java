package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.AggregationException;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;

public class IfNull extends Expression implements FieldHolder<IfNull> {
    private Expression target;
    private Expression replacement;
    private DocumentExpression document;

    public IfNull() {
        super("$ifNull");
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());
        writer.writeStartArray();
        expression(mapper, writer, target, encoderContext);
        expression(mapper, writer, replacement, encoderContext);
        expression(mapper, writer, document, encoderContext);
        writer.writeEndArray();
        writer.writeEndDocument();
    }

    @Override
    public IfNull field(String name, Expression expression) {
        if (replacement != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(getOperation()));
        }
        if (document == null) {
            document = Expressions.of();
        }
        document.field(name, expression);

        return this;
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

    public IfNull replacement(Expression replacement) {
        this.replacement = replacement;
        return this;
    }

    public IfNull target(Expression target) {
        this.target = target;
        return this;
    }
}
