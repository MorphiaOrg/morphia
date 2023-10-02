package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.Fields;
import dev.morphia.aggregation.expressions.impls.PipelineField;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.aggregation.stages.Group.GroupId;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

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
        document(writer, () -> {
            GroupId id = group.getId();
            CodecRegistry codecRegistry = getCodecRegistry();
            if (id != null) {
                writer.writeName("_id");
                DocumentExpression document = id.getDocument();
                if (document != null) {
                    Codec<DocumentExpression> codec = codecRegistry.get(DocumentExpression.class);
                    codec.encode(writer, document, encoderContext);
                } else {
                    Expression field = id.getField();
                    if (field != null) {
                        Codec codec = codecRegistry.get(field.getClass());
                        document(writer, () -> {
                            codec.encode(writer, field, encoderContext);
                        });
                    }
                }
            } else {
                writer.writeNull("_id");
            }

            Fields<Group> fields = group.getFields();
            if (fields != null) {
                MorphiaDatastore datastore = getDatastore();
                for (PipelineField field : fields.getFields()) {

                    String name = field.getName();
                    Expression value = field.getValue();
                    encodeIfNotNull(codecRegistry, writer, name, value, encoderContext);
                }
            }

        });
    }
}
