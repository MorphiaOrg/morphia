package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class DocumentExpression extends Expression implements FieldHolder<DocumentExpression> {
    private Fields<DocumentExpression> fields = Fields.on(this);

    public DocumentExpression() {
        super(null);
    }

    public void encode(final String name, final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument(name);
        fields.encode(mapper, writer, encoderContext);
        writer.writeEndDocument();
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        fields.encode(mapper, writer, encoderContext);
        writer.writeEndDocument();
    }

    @Override
    public DocumentExpression field(final String name, final Expression expression) {
        return fields.add(name, expression);
    }
}
