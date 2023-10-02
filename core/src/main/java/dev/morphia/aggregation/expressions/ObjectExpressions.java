package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;

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

    /**
     * Defines the values to be merged.
     */
    public static class MergeObjects extends Expression {

        protected MergeObjects() {
            super("$mergeObjects", new ExpressionList());
        }

        /**
         * Adds an expression to be merged
         *
         * @param expression the expression
         * @return this
         */
        @SuppressWarnings("unchecked")
        public MergeObjects add(Expression expression) {
            ExpressionList value = (ExpressionList) value();
            if (value != null) {
                value.add(expression);
            }
            return this;
        }
    }
}
