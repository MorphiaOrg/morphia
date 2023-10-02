package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

public class DocumentExpression extends Expression implements SingleValuedExpression, FieldHolder<DocumentExpression> {
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
     */
    @MorphiaInternal
    @Nullable
    public Fields fields() {
        return fields;
    }

    @Override
    public DocumentExpression field(String name, Expression expression) {
        return fields.add(name, expression);
    }
}
