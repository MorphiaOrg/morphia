package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

/**
 * Defines a document expression
 */
public class DocumentExpression extends Expression implements SimpleExpression, FieldHolder<DocumentExpression> {
    private final Fields fields = Fields.on(this);

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public DocumentExpression() {
        super("unused");
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the fields
     */
    @MorphiaInternal
    @Nullable
    public Fields fields() {
        return fields;
    }

    @Override
    public DocumentExpression field(String name, Object expression) {
        return fields.add(name, wrap(expression));
    }
}
