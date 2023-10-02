package dev.morphia.aggregation.expressions.impls;

import java.util.regex.Pattern;

import com.mongodb.lang.Nullable;
import dev.morphia.MorphiaDatastore;

import org.bson.BsonRegularExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

public class RegexExpression extends Expression {
    private final Expression input;
    private String regex;
    private String options;

    public RegexExpression(String operation, Expression input) {
        super(operation);
        this.input = input;
    }

    public Expression input() {
        return input;
    }

    @Nullable
    public String regex() {
        return regex;
    }

    @Nullable
    public String options() {
        return options;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
/*
        document(writer, operation(), () -> {
            expression(datastore, writer, "input", input, encoderContext);
            value(datastore, writer, "regex", new BsonRegularExpression(regex), encoderContext);
            value(writer, "options", options);
        });
*/
    }

    /**
     * Optional options to apply to the regex
     *
     * @param options the options
     * @return this
     */
    public RegexExpression options(String options) {
        this.options = options;
        return this;
    }

    /**
     * The regular expression
     *
     * @param pattern the regular expression
     * @return this
     */
    public RegexExpression pattern(String pattern) {
        this.regex = pattern;
        return this;
    }

    /**
     * The regular expression
     *
     * @param pattern the regular expression
     * @return this
     */
    public RegexExpression pattern(Pattern pattern) {
        this.regex = pattern.pattern();
        return this;
    }
}
