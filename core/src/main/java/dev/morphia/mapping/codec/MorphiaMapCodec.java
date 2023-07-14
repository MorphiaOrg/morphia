package dev.morphia.mapping.codec;

import java.util.Map;
import java.util.Map.Entry;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.MapCodec;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.internal.DatastoreHolder.holder;

/**
 * Maps Map subtypes to/from the database. This is mostly a pass-through to the driver codec except for the encoding, non-String types
 * are converted to Strings if possible.
 *
 * @morphia.internal
 * @since 2.1.7
 */
@MorphiaInternal
public class MorphiaMapCodec extends MapCodec {

    MorphiaMapCodec() {
    }

    @Override
    public void encode(BsonWriter writer, Map map, EncoderContext encoderContext) {
        document(writer, () -> {
            for (Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
                final Object key = entry.getKey();
                writer.writeName(Conversions.convert(key, String.class));
                if (entry.getValue() == null) {
                    writer.writeNull();
                } else {
                    Codec codec = holder.get().getCodecRegistry().get(entry.getValue().getClass());
                    codec.encode(writer, entry.getValue(), encoderContext);
                }
            }
        });
    }
}
