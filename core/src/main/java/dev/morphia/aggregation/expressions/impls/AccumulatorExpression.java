package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

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

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, operation(), () -> {
            writer.writeString("init", initFunction);
            array(datastore, writer, "initArgs", initArgs, encoderContext);
            writer.writeString("accumulate", accumulateFunction);
            array(datastore, writer, "accumulateArgs", accumulateArgs, encoderContext);
            writer.writeString("merge", mergeFunction);
            writer.writeString("finalize", finalizeFunction);
            writer.writeString("lang", lang);
        });
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
