package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.aggregation.AggregationException;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.sofia.Sofia;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;

public class IfNull extends Expression implements FieldHolder<IfNull> {
    private Expression target;
    private Expression replacement;
    private DocumentExpression document;

    public IfNull() {
        super("$ifNull");
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        array(writer, getOperation(), () -> {
            wrapExpression(datastore, writer, target, encoderContext);
            wrapExpression(datastore, writer, replacement, encoderContext);
            expression(datastore, writer, document, encoderContext);
        });
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
