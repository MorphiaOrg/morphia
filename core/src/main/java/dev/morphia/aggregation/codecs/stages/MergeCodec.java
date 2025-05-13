package dev.morphia.aggregation.codecs.stages;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.Merge;
import dev.morphia.annotations.internal.MorphiaInternal;

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
        String collection = merge.type() != null
                ? getDatastore().getMapper().getEntityModel(merge.type()).collectionName()
                : merge.collection();
        if (allDefaults(merge)) {
            writer.writeString(collection);
        } else {
            document(writer, () -> {
                String database = merge.database();

                if (database == null) {
                    writer.writeString("into", collection);
                } else {
                    document(writer, "into", () -> {
                        writer.writeString("db", database);
                        writer.writeString("coll", collection);
                    });
                }

                List<String> on = merge.on();
                if (on != null) {
                    if (on.size() == 1) {
                        writer.writeString("on", on.get(0));
                    } else {
                        array(writer, "on", () -> on.forEach(writer::writeString));
                    }
                }
                Map<String, Expression> variables = merge.variables();
                if (variables != null) {
                    document(writer, "let", () -> {
                        CodecRegistry registry = getDatastore().getCodecRegistry();
                        for (Entry<String, Expression> entry : variables.entrySet()) {
                            encodeIfNotNull(registry, writer, entry.getKey(), entry.getValue(), encoderContext);
                        }
                    });
                }
                writeEnum(writer, "whenMatched", merge.getWhenMatched());
                value(getCodecRegistry(), writer, "whenMatched", merge.whenMatchedPipeline(), encoderContext);
                writeEnum(writer, "whenNotMatched", merge.whenNotMatched());
            });
        }
    }

    private void writeEnum(BsonWriter writer, String name, Enum<?> value) {
        if (value != null) {
            writer.writeString(name, value.name().toLowerCase());
        }
    }

    /**
     * @hidden
     * @return
     * @morphia.internal
     */
    @MorphiaInternal
    private boolean allDefaults(Merge merge) {
        return (merge.type() != null || merge.collection() != null)
                && merge.on() == null
                && merge.variables() == null
                && merge.getWhenMatched() == null
                && merge.whenMatchedPipeline() == null
                && merge.whenNotMatched() == null;
    }
}
