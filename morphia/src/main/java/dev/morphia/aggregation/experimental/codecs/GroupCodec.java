package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.stages.Expression;
import dev.morphia.aggregation.experimental.stages.Group;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.List;

public class GroupCodec extends StageCodec<Group> {
    private CodecRegistry codecRegistry;

    public GroupCodec(final CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final Group group, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        List<Expression> id = group.getId();
        if (id != null) {
            writer.writeName("_id");
            if(id.size() > 1) {
                writer.writeStartDocument();
                encodeExpressions(writer, id, encoderContext);
                writer.writeEndDocument();
            } else {
                encodeExpressions(writer, id, encoderContext);
            }
        } else {
            writer.writeNull("_id");
        }

        encodeExpressions(writer, group.getFields(), encoderContext);

        writer.writeEndDocument();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void encodeExpressions(final BsonWriter writer, final List<Expression> expressions, final EncoderContext encoderContext) {
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
