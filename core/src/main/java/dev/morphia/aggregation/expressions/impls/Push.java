package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.AggregationException;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

public class Push extends Expression implements FieldHolder<Push> {
    private Expression field;
    private DocumentExpression document;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Push() {
        super("$push");
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public Expression field() {
        return field;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public DocumentExpression document() {
        return document;
    }

    @Override
    public Push field(String name, Expression expression) {
        if (field != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(operation()));
        }
        if (document == null) {
            document = Expressions.document();
        }
        document.field(name, expression);

        return this;
    }

    public Push single(Expression source) {
        if (document != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(operation()));
        }
        this.field = source;
        return this;
    }
}
