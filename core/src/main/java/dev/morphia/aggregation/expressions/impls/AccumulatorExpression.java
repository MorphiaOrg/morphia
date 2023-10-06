package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @since 2.1
 */
public class AccumulatorExpression extends Expression {
    private final String initFunction;
    private final String accumulateFunction;
    private final List<Expression> accumulateArgs;
    private final String mergeFunction;
    private final String lang = "js";
    private List<Expression> initArgs;
    private String finalizeFunction;

    /**
     * @param initFunction       the initial function
     * @param accumulateFunction the accumulate function
     * @param accumulateArgs     the accumulator arguments
     * @param mergeFunction      the merge function
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public AccumulatorExpression(String initFunction, String accumulateFunction, List<Expression> accumulateArgs, String mergeFunction) {
        super("$accumulator");
        this.initFunction = initFunction;
        this.accumulateFunction = accumulateFunction;
        this.accumulateArgs = accumulateArgs;
        this.mergeFunction = mergeFunction;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the initializer function
     */
    @MorphiaInternal
    public String initFunction() {
        return initFunction;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the accumulator function
     */
    @MorphiaInternal
    public String accumulateFunction() {
        return accumulateFunction;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the accumulator arguments
     */
    @MorphiaInternal
    public List<Expression> accumulateArgs() {
        return accumulateArgs;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the merge function
     */
    @MorphiaInternal
    public String mergeFunction() {
        return mergeFunction;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the function language
     */
    @MorphiaInternal
    public String lang() {
        return lang;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the arguments passed to the init function.
     */
    @MorphiaInternal
    public List<Expression> initArgs() {
        return initArgs;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the finalize function
     */
    @MorphiaInternal
    public String finalizeFunction() {
        return finalizeFunction;
    }

    /**
     * Optional. Function used to update the result of the accumulation.
     *
     * @param finalizeFunction the function body
     * @return this
     */
    public AccumulatorExpression finalizeFunction(String finalizeFunction) {
        this.finalizeFunction = finalizeFunction;
        return this;
    }

    /**
     * Optional. Arguments passed to the init function.
     *
     * @param initArgs the arguments
     * @return this
     */
    public AccumulatorExpression initArgs(List<Expression> initArgs) {
        this.initArgs = initArgs;
        return this;
    }

}
