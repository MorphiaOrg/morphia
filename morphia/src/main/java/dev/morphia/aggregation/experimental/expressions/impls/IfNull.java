package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.AggregationException;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeUnnamedExpression;

public class IfNull extends Expression implements FieldHolder<IfNull> {
    private Expression target;
    private Expression replacement;
    private DocumentExpression document;

    public IfNull() {
        super("$ifNull");
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());
        writer.writeStartArray();
        writeUnnamedExpression(mapper, writer, target, encoderContext);
        writeUnnamedExpression(mapper, writer, replacement, encoderContext);
        writeUnnamedExpression(mapper, writer, document, encoderContext);
        writer.writeEndArray();
        writer.writeEndDocument();
    }

    @Override
    public IfNull field(final String name, final Expression expression) {
        if (replacement != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(getOperation()));
        }
        if (document == null) {
            document = Expression.of();
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

    public IfNull replacement(final Expression replacement) {
        this.replacement = replacement;
        return this;
    }

    public IfNull target(final Expression target) {
        this.target = target;
        return this;
    }
}
