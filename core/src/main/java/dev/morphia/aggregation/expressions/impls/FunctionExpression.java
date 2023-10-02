package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.MorphiaDatastore;
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

    public String body() {
        return body;
    }

    public List<Expression> args() {
        return args;
    }

    public String lang() {
        return lang;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, operation(), () -> {
            writer.writeString("body", body);
            ExpressionHelper.array(datastore, writer, "args", args, encoderContext);
            writer.writeString("lang", lang);
        });
    }
}
