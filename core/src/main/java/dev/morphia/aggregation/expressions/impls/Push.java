package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.AggregationException;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

/**
 * Returns an array of all values that result from applying an expression to each document in a group of documents that share the
 * same group by key.
 */
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
     * @return the field
     */
    @MorphiaInternal
    @Nullable
    public Expression field() {
        return field;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the document
     */
    @MorphiaInternal
    @Nullable
    public DocumentExpression document() {
        return document;
    }

    @Override
    public Push field(String name, Object expression) {
        if (field != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(operation()));
        }
        if (document == null) {
            document = Expressions.document();
        }
        document.field(name, expression);

        return this;
    }

    /**
     * Pushes a single, unnamed value
     * 
     * @param value the value to push
     * @return this
     */
    public Push single(Object value) {
        if (document != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(operation()));
        }
        this.field = Expressions.wrap(value);
        return this;
    }
}
