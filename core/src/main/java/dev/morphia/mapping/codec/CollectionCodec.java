package dev.morphia.mapping.codec;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.function.Supplier;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;

import static java.lang.String.format;

/**
 * A codec for Collection type
 *
 * @param <T> parameterized type of the Collection
 */
public class CollectionCodec<T> implements Codec<Collection<T>> {
    private final Class<Collection<T>> encoderClass;
    private final Codec<T> codec;
    // creates the collections this codec decodes into; worked out on first use, so an unsupported type still fails
    // when decoding rather than when the codec is built
    private volatile Supplier<Collection<T>> factory;

    protected CollectionCodec(Class<Collection<T>> encoderClass, Codec<T> codec) {
        this.encoderClass = encoderClass;
        this.codec = codec;
    }

    protected Codec<T> getCodec() {
        return codec;
    }

    @Override
    public void encode(BsonWriter writer, Collection<T> collection, EncoderContext encoderContext) {
        writer.writeStartArray();
        for (T value : collection) {
            if (value == null) {
                writer.writeNull();
            } else {
                codec.encode(writer, value, encoderContext);
            }
        }
        writer.writeEndArray();
    }

    @Override
    public Collection<T> decode(BsonReader reader, DecoderContext context) {
        Collection<T> collection = getInstance();
        reader.readStartArray();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            if (reader.getCurrentBsonType() == BsonType.NULL) {
                collection.add(null);
                reader.readNull();
            } else {
                collection.add(codec.decode(reader, context));
            }
        }
        reader.readEndArray();
        return collection;
    }

    @Override
    public Class<Collection<T>> getEncoderClass() {
        return encoderClass;
    }

    protected Collection<T> getInstance() {
        Supplier<Collection<T>> supplier = factory;
        if (supplier == null) {
            supplier = factory();
            factory = supplier;
        }
        return supplier.get();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Supplier<Collection<T>> factory() {
        if (encoderClass.isInterface()) {
            if (encoderClass.isAssignableFrom(ArrayList.class)) {
                return ArrayList::new;
            } else if (encoderClass.isAssignableFrom(HashSet.class)) {
                return HashSet::new;
            } else {
                throw new CodecConfigurationException(format("Unsupported Collection interface of %s!", encoderClass.getName()));
            }
        }
        Class<?> type = encoderClass;
        if (type == ArrayList.class) {
            return ArrayList::new;
        } else if (type == HashSet.class) {
            return HashSet::new;
        } else if (type == LinkedHashSet.class) {
            return LinkedHashSet::new;
        } else if (type == LinkedList.class) {
            return LinkedList::new;
        }
        try {
            Constructor<Collection<T>> constructor = encoderClass.getDeclaredConstructor();
            return () -> {
                try {
                    return constructor.newInstance();
                } catch (Exception e) {
                    throw new CodecConfigurationException(e.getMessage(), e);
                }
            };
        } catch (Exception e) {
            throw new CodecConfigurationException(e.getMessage(), e);
        }
    }
}
