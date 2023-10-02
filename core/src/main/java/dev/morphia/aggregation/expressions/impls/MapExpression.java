package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;
import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;

public class MapExpression extends Expression {
    private final Expression input;
    private final Expression in;
    private String as;

    public MapExpression(Expression input, Expression in) {
        super("$map");
        this.input = input;
        this.in = in;
    }

    public MapExpression as(String as) {
        this.as = as;
        return this;
    }

    public Expression input() {
        return input;
    }

    public Expression in() {
        return in;
    }

    @Nullable
    public String as() {
        return as;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }
}
