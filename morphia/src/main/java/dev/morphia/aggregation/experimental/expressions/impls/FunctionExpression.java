package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.writeNamedValue;

public class FunctionExpression extends Expression {
    private final String body;
    private final Expression[] args;
    private final String lang = "js";

    public FunctionExpression(String body, Expression[] args) {
        super("$function");
        this.body = body;
        this.args = args;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, getOperation(), () -> {
                writeNamedValue(mapper, writer, "body", body, encoderContext);
                writeNamedValue(mapper, writer, "args", args, encoderContext);
                writeNamedValue(mapper, writer, "lang", lang, encoderContext);
            });
        });
    }
}
