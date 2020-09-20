package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.writeNamedValue;


public class AccumulatorExpression extends Expression {
    private final String initFunction;
    private final String accumulateFunction;
    private final List<Expression> accumulateArgs;
    private final String mergeFunction;
    private final String lang = "js";
    private List<Expression> initArgs;
    private String finalizeFunction;


    /**
     * @morphia.internal
     */
    public AccumulatorExpression(String initFunction, String accumulateFunction, List<Expression> accumulateArgs, String mergeFunction) {
        super("$accumulator");
        this.initFunction = initFunction;
        this.accumulateFunction = accumulateFunction;
        this.accumulateArgs = accumulateArgs;
        this.mergeFunction = mergeFunction;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, getOperation(), () -> {
                writeNamedValue(mapper, writer, "init", initFunction, encoderContext);
                writeNamedValue(mapper, writer, "initArgs", initArgs, encoderContext);
                writeNamedValue(mapper, writer, "accumulate", accumulateFunction, encoderContext);
                writeNamedValue(mapper, writer, "accumulateArgs", accumulateArgs, encoderContext);
                writeNamedValue(mapper, writer, "merge", mergeFunction, encoderContext);
                writeNamedValue(mapper, writer, "finalize", finalizeFunction, encoderContext);
                writeNamedValue(mapper, writer, "lang", lang, encoderContext);
            });
        });
    }

    public AccumulatorExpression finalizeFunction(String finalizeFunction) {
        this.finalizeFunction = finalizeFunction;
        return this;
    }

    public AccumulatorExpression initArgs(List<Expression> initArgs) {
        this.initArgs = initArgs;
        return this;
    }

}
