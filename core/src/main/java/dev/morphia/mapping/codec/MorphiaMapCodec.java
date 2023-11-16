package dev.morphia.mapping.codec;

import java.util.Map;
import java.util.Map.Entry;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

/**
 * Maps Map subtypes to/from the database. This is mostly a pass-through to the driver codec except for the encoding, non-String types
 * are converted to Strings if possible.
 *
 * @hidden
 * @morphia.internal
 * @since 2.1.7
 */
@MorphiaInternal
@SuppressWarnings("rawtypes")
public class MorphiaMapCodec implements Codec<Map> {

    private MorphiaDatastore datastore;

    MorphiaMapCodec(MorphiaDatastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public Map decode(BsonReader bsonReader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
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
                    Codec codec = datastore.getCodecRegistry().get(entry.getValue().getClass());
                    codec.encode(writer, entry.getValue(), encoderContext);
                }
            }
        });
    }

    @Override
    public Class<Map> getEncoderClass() {
        return Map.class;
    }
}
