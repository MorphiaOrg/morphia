package dev.morphia.aggregation.codecs.stages;

import java.util.List;
import java.util.Locale;

import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.SetWindowFields;
import dev.morphia.aggregation.stages.SetWindowFields.Output;
import dev.morphia.aggregation.stages.SetWindowFields.Window;
import dev.morphia.query.Sort;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

public class SetWindowFieldsCodec extends StageCodec<SetWindowFields> {

    private final Codec<Object> objectCodec;

    public SetWindowFieldsCodec(Datastore datastore) {
        super(datastore);
        objectCodec = getDatastore().getCodecRegistry()
                .get(Object.class);
    }

    @Override
    public Class<SetWindowFields> getEncoderClass() {
        return SetWindowFields.class;
    }

    @Override
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
                for (Output output : value.outputs()) {
                    document(writer, output.name(), () -> {
                        operator(writer, encoderContext, output.operator());
                        window(writer, output, encoderContext);
                    });
                }
            });
        });
    }

    private void documents(BsonWriter writer, @Nullable List<Object> list, String documents,
            EncoderContext encoderContext) {
        if (list != null) {
            array(writer, documents, () -> {
                for (Object document : list) {
                    objectCodec.encode(writer, document, encoderContext);
                }
            });
        }
    }

    @SuppressWarnings("rawtypes")
    private void operator(BsonWriter writer, EncoderContext encoderContext, @Nullable Expression operator) {
        if (operator != null) {
            expression(getDatastore(), writer, operator, encoderContext);
        }
    }

    private void window(BsonWriter writer, Output output, EncoderContext encoderContext) {
        Window window = output.windowDef();
        if (window != null) {
            document(writer, "window", () -> {
                documents(writer, window.documents(), "documents", encoderContext);
                documents(writer, window.range(), "range", encoderContext);
                TimeUnit unit = window.unit();
                if (unit != null) {
                    writer.writeString("unit", unit.name().toLowerCase(Locale.ROOT));
                }
            });
        }
    }
}
