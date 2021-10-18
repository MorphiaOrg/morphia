package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.AggregationException;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.sofia.Sofia;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;

public class IfNull extends Expression implements FieldHolder<IfNull> {
    private Expression target;
    private Expression replacement;
    private DocumentExpression document;

    public IfNull() {
        super("$ifNull");
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> {
            array(writer, getOperation(), () -> {
                expression(datastore, writer, target, encoderContext);
                expression(datastore, writer, replacement, encoderContext);
                expression(datastore, writer, document, encoderContext);
            });
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
