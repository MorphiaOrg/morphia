package dev.morphia.aggregation.expressions.impls;

import java.util.regex.Pattern;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

public class RegexExpression extends Expression {
    private final Expression input;
    private String regex;
    private String options;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public RegexExpression(String operation, Expression input) {
        super(operation);
        this.input = input;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression input() {
        return input;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public String regex() {
        return regex;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public String options() {
        return options;
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
