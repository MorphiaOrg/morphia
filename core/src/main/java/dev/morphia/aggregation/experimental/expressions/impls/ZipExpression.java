package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.codecs.ExpressionHelper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

public class ZipExpression extends Expression {
    private final List<Expression> inputs;
    private ValueExpression useLongestLength;
    private Expression defaults;

    /**
     * @param inputs
     * @morphia.internal
     */
    public ZipExpression(List<Expression> inputs) {
        super("$zip");
        this.inputs = inputs;
    }

    /**
     * An array of default element values to use if the input arrays have different lengths. You must specify useLongestLength: true
     * along with this field, or else $zip will return an error.
     * <p>
     * If useLongestLength: true but defaults is empty or not specified, $zip uses null as the default value.
     * <p>
     * If specifying a non-empty defaults, you must specify a default for each input array or else $zip will return an error.
     *
     * @param defaults the defaults
     * @return this
     */
    public ZipExpression defaults(Expression defaults) {
        this.defaults = defaults;
        return this;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            array(datastore, writer, "inputs", inputs, encoderContext);
            ExpressionHelper.expression(datastore, writer, "useLongestLength", useLongestLength, encoderContext);
            ExpressionHelper.expression(datastore, writer, "defaults", defaults, encoderContext);
        });
    }

    /**
     * Specifies whether the length of the longest array determines the number of arrays in the output array.  The default on the server
     * is false.
     *
     * @param useLongestLength true to use the longest length
     * @return this
     */
    public ZipExpression useLongestLength(Boolean useLongestLength) {
        this.useLongestLength = new ValueExpression(useLongestLength);
        return this;
    }
}
