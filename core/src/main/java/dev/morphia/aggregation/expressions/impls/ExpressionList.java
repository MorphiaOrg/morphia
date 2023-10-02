package dev.morphia.aggregation.expressions.impls;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.lang.NonNull;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static java.util.Arrays.asList;

/**
 * Wraps a list of expressions as an Expression so we can generically deal with all values as Expressions
 *
 * @morphia.internal
 * @since 2.3
 */
@MorphiaInternal
public class ExpressionList extends Expression implements SingleValuedExpression {
    private final List<Expression> values;

    public ExpressionList(List<Expression> values) {
        super("unused");
        this.values = new ArrayList<>(values);
    }

    public ExpressionList(Expression... values) {
        super("unused");
        this.values = new ArrayList<>(asList(values));
    }

    @NonNull
    public static ExpressionList coalesce(Expression first, Expression... values) {
        ExpressionList expressionList = new ExpressionList(first);
        expressionList.values.addAll(asList(values));

        return expressionList;
    }

    public void add(Expression expression) {
        values.add(expression);
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
        /*
         * array(writer, () -> {
         * for (Expression expression : values) {
         * wrapExpression(datastore, writer, expression, encoderContext);
         * }
         * });
         */
    }

    @Override
    public Expression value() {
        throw new UnsupportedOperationException("should have called getValues() here");
    }

    @NonNull
    public List<Expression> getValues() {
        return values;
    }
}
