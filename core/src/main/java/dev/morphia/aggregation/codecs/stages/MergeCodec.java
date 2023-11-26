package dev.morphia.aggregation.codecs.stages;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.Merge;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.CodecHelper.value;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MergeCodec extends StageCodec<Merge> {
    public MergeCodec(MorphiaDatastore datastore) {
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
                    ? getDatastore().getMapper().getEntityModel(merge.getType()).collectionName()
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
                    CodecRegistry registry = getDatastore().getCodecRegistry();
                    for (Entry<String, Expression> entry : variables.entrySet()) {
                        encodeIfNotNull(registry, writer, entry.getKey(), entry.getValue(), encoderContext);
                    }
                });
            }
            writeEnum(writer, "whenMatched", merge.getWhenMatched());
            value(getCodecRegistry(), writer, "whenMatched", merge.getWhenMatchedPipeline(), encoderContext);
            writeEnum(writer, "whenNotMatched", merge.getWhenNotMatched());
        });
    }

    private void writeEnum(BsonWriter writer, String name, Enum<?> value) {
        if (value != null) {
            writer.writeString(name, value.name().toLowerCase());
        }
    }
}
