package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Sort;
import dev.morphia.aggregation.stages.Sort.SortType;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

public class SortCodec extends StageCodec<Sort> {
    public SortCodec(MorphiaDatastore datastore) {
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
                writer.writeName(sort.field());
                sort.direction().encode(writer);
            }
        });
    }
}
