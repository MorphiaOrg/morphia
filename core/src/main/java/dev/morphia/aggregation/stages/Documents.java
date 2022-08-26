package dev.morphia.aggregation.stages;

import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.annotations.internal.MorphiaInternal;

import java.util.List;

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

    protected Documents(List<DocumentExpression> expressions) {
        super("$documents");
        this.expressions = expressions;
    }

    public static Documents documents(DocumentExpression... expressions) {
        return new Documents(asList(expressions));
    }

    /**
     * @return the expressions
     * @morphia.internal
     */
    @MorphiaInternal
    public List<DocumentExpression> expressions() {
        return expressions;
    }
}
