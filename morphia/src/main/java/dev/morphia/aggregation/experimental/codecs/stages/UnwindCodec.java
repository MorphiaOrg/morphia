package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;

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
        if(!value.optionsPresent()) {
            value.getPath().encode(getMapper(), writer, encoderContext);
        } else {
            writer.writeStartDocument();
            expression(getMapper(), writer, "path", value.getPath(), encoderContext);
            writeNamedValue(writer, "includeArrayIndex", value.getIncludeArrayIndex(), encoderContext);
            writeNamedValue(writer, "preserveNullAndEmptyArrays", value.getPreserveNullAndEmptyArrays(), encoderContext);
            writer.writeEndDocument();
        }
    }
}
