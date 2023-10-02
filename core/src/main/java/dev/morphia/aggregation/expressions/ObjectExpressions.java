package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.MergeObjects;

/**
 * Defines helper methods for the object expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#object-expression-operators Object Expressions
 * @since 2.0
 */
public final class ObjectExpressions {
    private ObjectExpressions() {
    }

    /**
     * Combines multiple documents into a single document.
     *
     * @return the new expression
     * @aggregation.expression $mergeObjects
     */
    public static MergeObjects mergeObjects() {
        return new MergeObjects();
    }
}
