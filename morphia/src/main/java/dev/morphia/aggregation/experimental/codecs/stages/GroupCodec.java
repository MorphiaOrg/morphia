package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Group.GroupId;
import dev.morphia.aggregation.experimental.stages.PipelineField;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.List;

public class GroupCodec extends StageCodec<Group> {

    public GroupCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final Group group, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        GroupId id = group.getId();
        if (id != null) {
            if(id.getFields().size() > 1) {
                writer.writeName("_id");
                writer.writeStartDocument();
                encodeFields(writer, id.getFields(), encoderContext);
                writer.writeEndDocument();
            } else {
                encodeFields(writer, id.getFields(), encoderContext);
            }
        } else {
            writer.writeNull("_id");
        }

        encodeFields(writer, group.getFields(), encoderContext);

        writer.writeEndDocument();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void encodeFields(final BsonWriter writer, final List<PipelineField> fields, final EncoderContext encoderContext) {
        for (final PipelineField field : fields) {
            writer.writeName(field.getName());
            Codec codec = getCodecRegistry().get(field.getValue().getClass());
            encoderContext.encodeWithChildContext(codec, writer, field.getValue());
        }
    }

    @Override
    public Class<Group> getEncoderClass() {
        return Group.class;
    }
}
