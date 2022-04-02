package dev.morphia.aggregation.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.Merge;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MergeCodec extends StageCodec<Merge> {
    public MergeCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Merge> getEncoderClass() {
        return Merge.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Merge merge, EncoderContext encoderContext) {
        document(writer, () -> {
            String collection = merge.getType() != null
                                ? getDatastore().getMapper().getEntityModel(merge.getType()).getCollectionName()
                                : merge.getCollection();
            String database = merge.getDatabase();

            if (database == null) {
                writer.writeString("into", collection);
            } else {
                document(writer, "into", () -> {
                    writer.writeString("db", database);
                    writer.writeString("coll", collection);
                });
            }

            List<String> on = merge.getOn();
            if (on != null) {
                if (on.size() == 1) {
                    writer.writeString("on", on.get(0));
                } else {
                    array(writer, "on", () -> on.forEach(writer::writeString));
                }
            }
            Map<String, Expression> variables = merge.getVariables();
            if (variables != null) {
                document(writer, "let", () -> {
                    for (Entry<String, Expression> entry : variables.entrySet()) {
                        expression(getDatastore(), writer, entry.getKey(), entry.getValue(), encoderContext);
                    }
                });
            }
            writeEnum(writer, "whenMatched", merge.getWhenMatched());
            value(getDatastore(), writer, "whenMatched", merge.getWhenMatchedPipeline(), encoderContext);
            writeEnum(writer, "whenNotMatched", merge.getWhenNotMatched());
        });
    }

    private void writeEnum(BsonWriter writer, String name, Enum<?> value) {
        if (value != null) {
            writer.writeString(name, value.name().toLowerCase());
        }
    }
}
