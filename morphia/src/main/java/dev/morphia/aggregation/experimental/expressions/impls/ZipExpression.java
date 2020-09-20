package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.writeNamedValue;

public class ZipExpression extends Expression {
    private final List<Expression> inputs;
    private Boolean useLongestLength;
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
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());
        writeNamedValue(mapper, writer, "inputs", inputs, encoderContext);
        writeNamedValue(mapper, writer, "useLongestLength", useLongestLength, encoderContext);
        writeNamedValue(mapper, writer, "defaults", defaults, encoderContext);
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    /**
     * Specifies whether the length of the longest array determines the number of arrays in the output array.  The default on the server
     * is false.
     *
     * @param useLongestLength true to use the longest length
     * @return this
     */
    public ZipExpression useLongestLength(Boolean useLongestLength) {
        this.useLongestLength = useLongestLength;
        return this;
    }
}
