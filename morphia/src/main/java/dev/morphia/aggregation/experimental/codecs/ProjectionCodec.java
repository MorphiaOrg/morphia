package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.stages.Expression;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Projection.ProjectionField;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ProjectionCodec implements Codec<Projection> {
    private CodecRegistry codecRegistry;

    public ProjectionCodec(final CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Override
    public Projection decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(final BsonWriter writer, final Projection value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument("$project");
        for (final ProjectionField field : value.getFields()) {
            write(writer, field, encoderContext);
        }
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    private void write(final BsonWriter writer, final ProjectionField field, final EncoderContext encoderContext) {
        writer.writeName(field.getName());
        Class aClass = field.getValue().getClass();
        Codec codec = codecRegistry.get(aClass);
        writer.writeStartDocument();
        encoderContext.encodeWithChildContext(codec, writer, field.getValue());
        writer.writeEndDocument();
    }

    @Override
    public Class<Projection> getEncoderClass() {
        return Projection.class;
    }
}
