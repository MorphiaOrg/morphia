package dev.morphia.mapping.codec;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import dev.morphia.Datastore;
import dev.morphia.DatastoreImpl;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

/**
 * Maps Map subtypes to/from the database. This is mostly a pass-through to the driver codec except for the encoding, non-String types
 * are converted to Strings if possible.
 *
 * @morphia.internal
 * @since 2.1.7
 */
@SuppressWarnings("rawtypes")
@MorphiaInternal
public class MorphiaMapCodec implements Codec<Map> {

    private Supplier<Map> factory;

    private Datastore datastore;

    private final Codec<?> valueCodec;

    public MorphiaMapCodec(DatastoreImpl datastore, Class<?> clazz, Codec<?> valueCodec) {
        this.datastore = datastore;
        this.valueCodec = valueCodec;
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            factory = () -> {
                try {
                    return (Map) ctor.newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new MappingException(e.getMessage(), e);
                }
            };
        } catch (ReflectiveOperationException e) {
            factory = HashMap::new;
        }
    }

    @Override
    public Map decode(final BsonReader reader, final DecoderContext decoderContext) {
        Map map = factory.get();

        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();
            if (reader.getCurrentBsonType() == BsonType.NULL) {
                reader.readNull();
                map.put(fieldName, null);
            } else {
                map.put(fieldName, valueCodec.decode(reader, decoderContext));
            }
        }

        reader.readEndDocument();
        return Map.of();
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
        return null;
    }
}
