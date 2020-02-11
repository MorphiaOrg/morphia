package dev.morphia.query.experimental.filters;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.mapping.Mapper;
import org.bson.BsonRegularExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.regex.Pattern;

public class RegexFilter extends Filter {
    private String regex;
    private String options;

    public RegexFilter(final String field) {
        super("$regex", field, null);
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
        writer.writeStartDocument(field(mapper));
        if (isNot()) {
            writer.writeStartDocument("$not");
        }
        ExpressionCodec.writeNamedValue(mapper, writer, "$regex", new BsonRegularExpression(regex), context);
        ExpressionCodec.writeNamedValue(mapper, writer, "options", options, context);
        if (isNot()) {
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
    }

    /**
     * Optional options to apply to the regex
     *
     * @param options the options
     * @return this
     */
    public RegexFilter options(final String options) {
        this.options = options;
        return this;
    }

    /**
     * The regular expression
     *
     * @param pattern the regular expression
     * @return this
     */
    public RegexFilter pattern(final String pattern) {
        this.regex = pattern;
        return this;
    }

    /**
     * The regular expression
     *
     * @param pattern the regular expression
     * @return this
     */
    public RegexFilter pattern(final Pattern pattern) {
        this.regex = pattern.pattern();
        return this;
    }
}
