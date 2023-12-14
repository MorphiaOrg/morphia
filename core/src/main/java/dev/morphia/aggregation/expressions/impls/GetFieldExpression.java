package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Returns the value of a specified field from a document. If you don't specify an object, $getField returns the value of the field from
 * $$CURRENT.
 * <p>
 * You can use $getField to retrieve the value of fields with names that contain periods (.) or start with dollar signs ($).
 *
 * @mongodb.server.release 5.0
 * @since 3.0
 */
public class GetFieldExpression extends Expression {
    private Expression input;

    public GetFieldExpression(Expression field) {
        super("$getField", field);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression input() {
        return input;
    }

    /**
     * A valid expression that contains the field for which you want to return a value. input must resolve to an object, missing, null,
     * or undefined. If omitted, defaults to the document currently being processed in the pipeline ($$CURRENT).
     *
     * Default: $$CURRENT
     *
     * @param input the input expression
     * @return this
     */
    public GetFieldExpression input(Expression input) {
        this.input = input;
        return this;
    }
}
