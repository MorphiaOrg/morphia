package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.Datastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

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
    @MorphiaInternal
    public FunctionExpression(String body, List<Expression> args) {
        super("$function");
        this.body = body;
        this.args = args;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            writer.writeString("body", body);
            ExpressionHelper.array(datastore, writer, "args", args, encoderContext);
            writer.writeString("lang", lang);
        });
    }
}
