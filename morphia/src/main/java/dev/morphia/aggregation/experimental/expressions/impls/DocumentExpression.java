package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class DocumentExpression extends Expression implements FieldHolder<DocumentExpression> {
    private final Fields<DocumentExpression> fields = Fields.on(this);

    public DocumentExpression() {
        super(null);
    }

    public void encode(String name, Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument(name);
        fields.encode(mapper, writer, encoderContext);
        writer.writeEndDocument();
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        fields.encode(mapper, writer, encoderContext);
        writer.writeEndDocument();
    }

    @Override
    public DocumentExpression field(String name, Expression expression) {
        return fields.add(name, expression);
    }
}
