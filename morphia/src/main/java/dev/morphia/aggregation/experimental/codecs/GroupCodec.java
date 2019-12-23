package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.stages.Expression;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.sofia.Sofia;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.List;

public class GroupCodec implements Codec<Group> {
    private CodecRegistry codecRegistry;

    public GroupCodec(final CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Override
    public Group decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(final BsonWriter writer, final Group group, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName("$group");
        writer.writeStartDocument();
        String name = group.getName();
        List<Expression> id = group.getCompoundId();
        if (name != null && id != null) {
            throw new BsonInvalidOperationException(Sofia.mixedGroupIdDefinition());
        }
        if (name != null) {
            writer.writeString("_id", name);
        }
        if (id != null) {
            writer.writeStartDocument("_id");
            encodeExpressions(writer, id, encoderContext);
            writer.writeEndDocument();
        }

        encodeExpressions(writer, group.getExpressions(), encoderContext);

        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void encodeExpressions(final BsonWriter writer,
                                   final List<Expression> expressions,
                                   final EncoderContext encoderContext) {
        for (final Expression expression : expressions) {
            Codec codec = codecRegistry.get(expression.getClass());
            encoderContext.encodeWithChildContext(codec, writer, expression);
        }
    }

    @Override
    public Class<Group> getEncoderClass() {
        return Group.class;
    }
}
