package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

public class DocumentExpression extends Expression implements SingleValuedExpression, FieldHolder<DocumentExpression> {
    private final Fields<DocumentExpression> fields = Fields.on(this);

    public DocumentExpression() {
        super("unused");
    }

    public void encode(String name, Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, name, () -> fields.encode(datastore, writer, encoderContext));
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> fields.encode(datastore, writer, encoderContext));
    }

    @Override
    public DocumentExpression field(String name, Expression expression) {
        return fields.add(name, expression);
    }
}
