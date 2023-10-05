package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Fields;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.aggregation.stages.Group.GroupId;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class GroupCodec extends StageCodec<Group> {
    public GroupCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Group> getEncoderClass() {
        return Group.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Group group, EncoderContext encoderContext) {
        CodecRegistry codecRegistry = getCodecRegistry();
        document(writer, () -> {
            GroupId id = group.getId();
            if (id != null) {
                writer.writeName("_id");
                if (!encodeIfNotNull(getCodecRegistry(), writer, id.getDocument(), encoderContext)) {
                    encodeIfNotNull(getCodecRegistry(), writer, id.getField(), encoderContext);
                }
            } else {
                writer.writeNull("_id");
            }

            Fields fields = group.getFields();
            if (fields != null) {
                Codec<Fields> codec = codecRegistry.get(Fields.class);
                codec.encode(writer, fields, encoderContext);
            }
        });
    }
}
