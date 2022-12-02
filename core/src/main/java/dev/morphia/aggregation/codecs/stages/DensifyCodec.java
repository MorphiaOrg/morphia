package dev.morphia.aggregation.codecs.stages;

import java.util.List;
import java.util.Locale;

import dev.morphia.Datastore;
import dev.morphia.aggregation.stages.Densify;
import dev.morphia.aggregation.stages.Densify.Range;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

public class DensifyCodec extends StageCodec<Densify> {
    public DensifyCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Densify> getEncoderClass() {
        return Densify.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Densify value, EncoderContext encoderContext) {
        document(writer, () -> {
            value(getDatastore(), writer, "field", value.field(), encoderContext);
            document(writer, "range", () -> {
                Range range = value.range();
                value(getDatastore(), writer, "step", range.step(), encoderContext);
                value(getDatastore(), writer, "unit", range.unit().name().toLowerCase(Locale.ROOT), encoderContext);
                switch (range.type()) {
                    case BOUNDED:
                        value(getDatastore(), writer, "bounds", List.of(range.lowerBound(), range.upperBound()), encoderContext);
                        break;
                    case FULL:
                    case PARTITION:
                        value(getDatastore(), writer, "bounds", range.type().name().toLowerCase(Locale.ROOT), encoderContext);
                        break;
                }
            });

        });
    }
}
