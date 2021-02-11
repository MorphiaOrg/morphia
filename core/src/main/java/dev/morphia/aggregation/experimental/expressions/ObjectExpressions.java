package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

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
            super("$mergeObjects", new ArrayList<Expression>());
        }

        /**
         * Adds an expression to be merged
         *
         * @param expression the expression
         * @return this
         */
        @SuppressWarnings("unchecked")
        public MergeObjects add(Expression expression) {
            List<Expression> value = (List<Expression>) getValue();
            if (value != null) {
                value.add(expression);
            }
            return this;
        }

        @Override
        public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
            super.encode(mapper, writer, encoderContext);
        }
    }
}
