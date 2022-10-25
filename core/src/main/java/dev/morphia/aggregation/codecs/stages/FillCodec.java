package dev.morphia.aggregation.codecs.stages;

import java.util.List;
import java.util.Locale;

import dev.morphia.Datastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.Fill;
import dev.morphia.aggregation.stages.Fill.Method;
import dev.morphia.query.Sort;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

public class FillCodec extends StageCodec<Fill> {
    public FillCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Fill> getEncoderClass() {
        return Fill.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Fill fill, EncoderContext encoderContext) {
        document(writer, () -> {
            if (fill.partitionBy() != null) {
                expression(getDatastore(), writer, "partitionBy", fill.partitionBy(), encoderContext);
            }
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
                            expression(getDatastore(), writer, "value", (Expression) value, encoderContext);
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
