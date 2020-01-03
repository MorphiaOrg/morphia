package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.expressions.Fields;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Group.GroupId;
import dev.morphia.aggregation.experimental.expressions.PipelineField;
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
            Fields<GroupId> fields = id.getFields();
            if(fields.size() > 1) {
                writer.writeName("_id");
                writer.writeStartDocument();
                fields.encode(getMapper(), writer, encoderContext);
                writer.writeEndDocument();
            } else {
                fields.encode(getMapper(), writer, encoderContext);
            }
        } else {
            writer.writeNull("_id");
        }

        Fields<Group> fields = group.getFields();
        if(fields != null) {
            fields.encode(getMapper(), writer, encoderContext);
        }

        writer.writeEndDocument();
    }

    @Override
    public Class<Group> getEncoderClass() {
        return Group.class;
    }
}
