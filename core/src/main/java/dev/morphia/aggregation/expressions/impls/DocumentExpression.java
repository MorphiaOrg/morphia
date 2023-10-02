package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;
import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class DocumentExpression extends Expression implements SingleValuedExpression, FieldHolder<DocumentExpression> {
    private final Fields fields = Fields.on(this);

    public DocumentExpression() {
        super("unused");
    }

    @Nullable
    public Fields fields() {
        return fields;
    }

    @Override
    public DocumentExpression field(String name, Expression expression) {
        return fields.add(name, expression);
    }
}
