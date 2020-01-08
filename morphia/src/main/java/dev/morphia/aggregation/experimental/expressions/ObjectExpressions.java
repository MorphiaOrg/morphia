package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

public class ObjectExpressions {
    /**
     * Combines multiple documents into a single document.
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/mergeObjects $mergeObjects
     */
    public static MergeObjects mergeObjects() {
        return new MergeObjects();
    }

    public static class MergeObjects extends Expression {

        protected MergeObjects() {
            super("$mergeObjects", new ArrayList<Expression>());
        }

        @SuppressWarnings("unchecked")
        public MergeObjects add(final Expression expression) {
            ((List<Expression>) getValue()).add(expression);
            return this;
        }

        @Override
        public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
            super.encode(mapper, writer, encoderContext);
        }
    }
}
