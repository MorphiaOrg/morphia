package dev.morphia.aggregation.stages;

import java.util.List;

import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.annotations.internal.MorphiaInternal;

import static java.util.Arrays.asList;

/**
 * Returns literal documents from input values.
 *
 * @mongodb.server.release 5.1
 * @aggregation.expression $documents
 * @since 2.3
 */
public class Documents extends Stage {
    private final List<DocumentExpression> expressions;

    /**
     * @param expressions the document expressions
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Documents(List<DocumentExpression> expressions) {
        super("$documents");
        this.expressions = expressions;
    }

    /**
     * Creates a new stage with the given document expressions
     *
     * @param expressions the documents
     * @return the new stage
     */
    public static Documents documents(DocumentExpression... expressions) {
        return new Documents(asList(expressions));
    }

    /**
     * @return the expressions
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public List<DocumentExpression> expressions() {
        return expressions;
    }
}
