package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.Fields;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Group.GroupId;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

public class GroupCodec extends StageCodec<Group> {

    public GroupCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Group> getEncoderClass() {
        return Group.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Group group, EncoderContext encoderContext) {
        document(writer, () -> {
            GroupId id = group.getId();
            if (id != null) {
                writer.writeName("_id");
                if (id.getDocument() != null) {
                    id.getDocument().encode(getDatastore(), writer, encoderContext);
                } else {
                    if (id.getField() != null) {
                        id.getField().encode(getDatastore(), writer, encoderContext);
                    }
                }
            } else {
                writer.writeNull("_id");
            }

            Fields<Group> fields = group.getFields();
            if (fields != null) {
                fields.encode(getDatastore(), writer, encoderContext);
            }

        });
    }
}
