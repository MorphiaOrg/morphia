package dev.morphia.aggregation.codecs.stages;

import java.util.List;
import java.util.Locale;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
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

@SuppressWarnings({"unchecked", "rawtypes"})
public class SetWindowFieldsCodec extends StageCodec<SetWindowFields> {

    private Codec<Object> objectCodec;

    public SetWindowFieldsCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    public Codec<Object> getObjectCodec() {
        if (objectCodec == null) {
            objectCodec = getDatastore().getCodecRegistry()
                    .get(Object.class);
        }
        return objectCodec;
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

    private void documents(BsonWriter writer, String name, @Nullable List<Object> list,
                           EncoderContext encoderContext) {
        if (list != null) {
            array(writer, name, () -> {
                for (Object document : list) {
                    Codec codec = getCodecRegistry().get(document.getClass());
                    codec.encode(writer, document, encoderContext);
                }
            });
        }
    }

    @SuppressWarnings("rawtypes")
    private void operator(BsonWriter writer, EncoderContext encoderContext, @Nullable Expression operator) {
        if (operator != null) {
            Codec codec = getCodecRegistry().get(operator.getClass());
            codec.encode(writer, operator, encoderContext);
        }
    }

    private void window(BsonWriter writer, Output output, EncoderContext encoderContext) {
        Window window = output.windowDef();
        if (window != null) {
            document(writer, "window", () -> {
                documents(writer, "documents", window.documents(), encoderContext);
                documents(writer, "range", window.range(), encoderContext);
                TimeUnit unit = window.unit();
                if (unit != null) {
                    writer.writeString("unit", unit.name().toLowerCase(Locale.ROOT));
                }
            });
        }
    }
}
