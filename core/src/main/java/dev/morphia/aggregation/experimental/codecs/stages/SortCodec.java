package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.aggregation.experimental.stages.Sort.SortType;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

public class SortCodec extends StageCodec<Sort> {
    public SortCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Sort> getEncoderClass() {
        return Sort.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Sort value, EncoderContext encoderContext) {
        document(writer, () -> {
            for (SortType sort : value.getSorts()) {
                writer.writeName(sort.getField());
                sort.getDirection().encode(writer);
            }
        });
    }
}
