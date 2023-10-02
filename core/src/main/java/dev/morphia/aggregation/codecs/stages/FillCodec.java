package dev.morphia.aggregation.codecs.stages;

import java.util.List;
import java.util.Locale;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.Fill;
import dev.morphia.aggregation.stages.Fill.Method;
import dev.morphia.query.Sort;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.array;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.value;

public class FillCodec extends StageCodec<Fill> {
    public FillCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Fill> getEncoderClass() {
        return Fill.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Fill fill, EncoderContext encoderContext) {
        document(writer, () -> {
            CodecRegistry registry = getCodecRegistry();
            encodeIfNotNull(registry, writer, "partitionBy", fill.partitionBy(), encoderContext);
            List<String> partitionByFields = fill.partitionByFields();
            if (partitionByFields != null) {
                array(writer, "partitionByFields", () -> {
                    partitionByFields.forEach(writer::writeString);
                });
            }
            Sort[] sorts = fill.sortBy();
            if (sorts != null) {
                document(writer, "sortBy", () -> {
                    for (Sort sort : sorts) {
                        writer.writeInt64(sort.getField(), sort.getOrder());
                    }
                });
            }
            document(writer, "output", () -> {
                fill.fields().forEach((key, value) -> {
                    if (value instanceof Expression) {
                        document(writer, key, () -> {
                            encodeIfNotNull(registry, writer, "value", (Expression) value, encoderContext);
                        });
                    } else if (value instanceof Fill.Method) {
                        document(writer, key, () -> {
                            value(writer, "method", ((Method) value).name().toLowerCase(Locale.ROOT));
                        });
                    }
                });
            });
        });
    }
}
