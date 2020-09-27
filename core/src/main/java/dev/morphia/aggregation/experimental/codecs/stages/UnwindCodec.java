package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class UnwindCodec extends StageCodec<Unwind> {
    public UnwindCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<Unwind> getEncoderClass() {
        return Unwind.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Unwind value, EncoderContext encoderContext) {
        if (!value.optionsPresent()) {
            value.getPath().encode(getMapper(), writer, encoderContext);
        } else {
            document(writer, () -> {
                expression(getMapper(), writer, "path", value.getPath(), encoderContext);
                value(getMapper(), writer, "includeArrayIndex", value.getIncludeArrayIndex(), encoderContext);
                value(getMapper(), writer, "preserveNullAndEmptyArrays", value.getPreserveNullAndEmptyArrays(), encoderContext);
            });
        }
    }
}
