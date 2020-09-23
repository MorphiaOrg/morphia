package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

/**
 * @since 2.1
 */
public class FunctionExpression extends Expression {
    private final String body;
    private final List<Expression> args;
    private final String lang = "js";

    /**
     * Creates the new expression
     *
     * @param body the function definition
     * @param args the funcation arguments
     * @morphia.internal
     */
    public FunctionExpression(String body, List<Expression> args) {
        super("$function");
        this.body = body;
        this.args = args;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, getOperation(), () -> {
                value(mapper, writer, "body", body, encoderContext);
                value(mapper, writer, "args", args, encoderContext);
                value(mapper, writer, "lang", lang, encoderContext);
            });
        });
    }
}
