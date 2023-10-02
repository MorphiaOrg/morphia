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
     * @param initFunction
     * @param accumulateFunction
     * @param accumulateArgs
     * @param mergeFunction
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

    public String initFunction() {
        return initFunction;
    }

    public String accumulateFunction() {
        return accumulateFunction;
    }

    public List<Expression> accumulateArgs() {
        return accumulateArgs;
    }

    public String mergeFunction() {
        return mergeFunction;
    }

    public String lang() {
        return lang;
    }

    public List<Expression> initArgs() {
        return initArgs;
    }

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
