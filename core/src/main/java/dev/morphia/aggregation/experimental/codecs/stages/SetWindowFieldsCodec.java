package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.stages.SetWindowFields;
import dev.morphia.aggregation.experimental.stages.SetWindowFields.Output;
import dev.morphia.query.Sort;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.List;
import java.util.Locale;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class SetWindowFieldsCodec extends StageCodec<SetWindowFields> {
    public SetWindowFieldsCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<SetWindowFields> getEncoderClass() {
        return SetWindowFields.class;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void encodeStage(BsonWriter writer, SetWindowFields value, EncoderContext encoderContext) {
        document(writer, () -> {
            if (value.partition() != null) {
                writer.writeName("partitionBy");
                expression(getDatastore(), writer, value.partition(), encoderContext);
            }
            Sort[] sorts = value.sorts();
            if (sorts != null) {
                document(writer, "sortBy", () -> {
                    for (Sort sort : sorts) {
                        writer.writeInt64(sort.getField(), sort.getOrder());
                    }
                });
            }
            document(writer, "output", () -> {
                Codec<Object> objectCodec = getDatastore().getCodecRegistry()
                                                          .get(Object.class);
                for (Output output : value.outputs()) {
                    document(writer, output.name(), () -> {
                        Expression operator = output.operator();
                        if (operator != null) {
                            writer.writeName(operator.getOperation());
                            Object value1 = operator.getValue();
                            if (value1 instanceof List) {
                                List list = (List) value1;
                                if (list.size() == 1) {
                                    value(getDatastore(), writer, list.get(0), encoderContext);
                                } else {
                                    array(writer, () -> {
                                        for (Object o : list) {
                                            value(getDatastore(), writer, o, encoderContext);
                                        }
                                    });
                                }
                            } else {
                                value(getDatastore(), writer, value1, encoderContext);
                            }
                        }
                        if (output.windowDef() != null) {
                            document(writer, "window", () -> {
                                if (output.windowDef().documents() != null) {
                                    array(writer, "documents", () -> {
                                        for (Object document : output.windowDef().documents()) {
                                            objectCodec.encode(writer, document, encoderContext);
                                        }
                                    });
                                }
                                if (output.windowDef().range() != null) {
                                    array(writer, "range", () -> {
                                        for (Object document : output.windowDef().range()) {
                                            objectCodec.encode(writer, document, encoderContext);
                                        }
                                    });
                                }
                                if (output.windowDef().unit() != null) {
                                    writer.writeString("unit", output.windowDef().unit().name().toLowerCase(Locale.ROOT));
                                }
                            });
                        }
                    });
                }
            });
        });
    }
}
