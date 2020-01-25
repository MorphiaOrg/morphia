package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.mapping.Mapper;
import org.bson.BsonRegularExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.regex.Pattern;

public class RegexExpression extends Expression {
    private final Expression input;
    private String regex;
    private String options;

    public RegexExpression(final String operation, final Expression input) {
        super(operation);
        this.input = input;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());
        ExpressionCodec.writeNamedExpression(mapper, writer, "input", input, encoderContext);
        ExpressionCodec.writeNamedValue(mapper, writer, "regex", new BsonRegularExpression(regex), encoderContext);
        ExpressionCodec.writeNamedValue(mapper, writer, "options", options, encoderContext);
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    /**
     * Optional options to apply to the regex
     *
     * @param options the options
     * @return this
     */
    public RegexExpression options(final String options) {
        this.options = options;
        return this;
    }

    /**
     * The regular expression
     *
     * @param pattern the regular expression
     * @return this
     */
    public RegexExpression regex(final String pattern) {
        this.regex = pattern;
        return this;
    }

    /**
     * The regular expression
     *
     * @param pattern the regular expression
     * @return this
     */
    public RegexExpression regex(final Pattern pattern) {
        this.regex = pattern.pattern();
        return this;
    }
}
