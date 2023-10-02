package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;
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
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IfNull field(String name, Expression expression) {
        if (replacement != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(operation()));
        }
        if (document == null) {
            document = Expressions.of();
        }
        document.field(name, expression);

        return this;
    }

    public IfNull replacement(Expression replacement) {
        this.replacement = replacement;
        return this;
    }

    public IfNull target(Expression target) {
        this.target = target;
        return this;
    }

    public Expression target() {
        return target;
    }

    public Expression replacement() {
        return replacement;
    }

    public DocumentExpression document() {
        return document;
    }
}
