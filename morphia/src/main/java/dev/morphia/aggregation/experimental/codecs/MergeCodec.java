package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.codecs.stages.StageCodec;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.stages.Merge;
import dev.morphia.aggregation.experimental.stages.Stage;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MergeCodec extends StageCodec<Merge> {
    public MergeCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<Merge> getEncoderClass() {
        return Merge.class;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final Merge value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        String collection;
        String database = null;
        if(value.getType() != null) {
            collection = getMapper().getMappedClass(value.getType()).getCollectionName();
        } else {
            collection = value.getCollection();
            database = value.getDatabase();
        }

        if(database == null) {
            writer.writeString("into", collection);
        } else {
            writer.writeStartDocument("into");
            writer.writeString("db", database);
            writer.writeString("coll", collection);
            writer.writeEndDocument();
        }

        List<String> on = value.getOn();
        if(on != null) {
            if(on.size() == 1) {
                writer.writeString("on", on.get(0));
            } else {
                writer.writeStartArray("on");
                for (final String name : on) {
                    writer.writeString(name);
                }
                writer.writeEndArray();
            }
        }
        Map<String, Expression> variables = value.getVariables();
        if(variables != null) {
            writer.writeStartDocument("let");
            for (final Entry<String, Expression> entry : variables.entrySet()) {
                writer.writeName(entry.getKey());
                entry.getValue().encode(getMapper(), writer, encoderContext);
            }
            writer.writeEndDocument();
        }
        writeEnum(writer, "whenMatched", value.getWhenMatched(), encoderContext);
        List<Stage> pipeline = value.getWhenMatchedPipeline();
        if(pipeline != null) {
            writer.writeName("whenMatched");
            Codec codec = getCodecRegistry().get(pipeline.getClass());
            encoderContext.encodeWithChildContext(codec, writer, pipeline);
        }
        writeEnum(writer, "whenNotMatched", value.getWhenNotMatched(), encoderContext);
        writer.writeEndDocument();
    }

    private void writeEnum(final BsonWriter writer, final String name, final Enum value, final EncoderContext encoderContext) {
        if (value != null) {
            writer.writeString(name, value.name().toLowerCase());
        }
    }
}
