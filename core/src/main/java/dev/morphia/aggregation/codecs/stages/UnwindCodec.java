package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.Unwind;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class UnwindCodec extends StageCodec<Unwind> {
    public UnwindCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Unwind> getEncoderClass() {
        return Unwind.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Unwind value, EncoderContext encoderContext) {
        Expression path = value.getPath();
        if (!value.optionsPresent()) {
            Codec codec = getCodecRegistry().get(path.getClass());
            codec.encode(writer, path, encoderContext);
        } else {
            document(writer, () -> {
                encodeIfNotNull(getCodecRegistry(), writer, "path", path, encoderContext);
                value(writer, "includeArrayIndex", value.getIncludeArrayIndex());
                value(writer, "preserveNullAndEmptyArrays", value.getPreserveNullAndEmptyArrays());
            });
        }
    }
}
