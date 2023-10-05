package dev.morphia.aggregation.codecs.stages;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Densify;
import dev.morphia.aggregation.stages.Densify.Range;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class DensifyCodec extends StageCodec<Densify> {
    public DensifyCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Densify> getEncoderClass() {
        return Densify.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Densify value, EncoderContext encoderContext) {
        document(writer, () -> {
            CodecRegistry registry = getDatastore().getCodecRegistry();
            value(writer, "field", value.field());
            document(writer, "range", () -> {
                Range range = value.range();
                value(registry, writer, "step", range.step(), encoderContext);
                value(writer, "unit", range.unit());
                switch (range.type()) {
                    case BOUNDED -> value(registry, writer, "bounds", List.of(range.lowerBound(), range.upperBound()), encoderContext);
                    case FULL, PARTITION -> value(writer, "bounds", range.type());
                }
            });

        });
    }
}
