package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class ArrayLiteral extends ArrayExpression {

    private Object[] objects;

    public ArrayLiteral(Expression... values) {
        super("unused", asList(values));
    }

    public ArrayLiteral(Object... objects) {
        super("unused", emptyList());
        this.objects = objects;
    }

    @Nullable
    public Object[] objects() {
        return objects;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        if (objects == null) {
            Expression value = value();
            if (value != null) {
                value.encode(datastore, writer, encoderContext);
            }
        } else {
            array(writer, () -> {
                for (Object object : objects) {
                    ExpressionHelper.value(datastore, writer, object, encoderContext);
                }
            });
        }
    }
}
