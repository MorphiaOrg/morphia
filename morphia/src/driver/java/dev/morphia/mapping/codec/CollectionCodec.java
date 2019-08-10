package dev.morphia.mapping.codec;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import static java.lang.String.format;

public class CollectionCodec<T> implements Codec<Collection<T>> {
    private final Class<Collection<T>> encoderClass;
    private final Codec<T> codec;

    protected CollectionCodec(final Class<Collection<T>> encoderClass, final Codec<T> codec) {
        this.encoderClass = encoderClass;
        this.codec = codec;
    }

    protected Codec<T> getCodec() {
        return codec;
    }

    @Override
    public void encode(final BsonWriter writer, final Collection<T> collection, final EncoderContext encoderContext) {
        writer.writeStartArray();
        for (final T value : collection) {
            if (value == null) {
                writer.writeNull();
            } else {
                codec.encode(writer, value, encoderContext);
            }
        }
        writer.writeEndArray();
    }

    @Override
    public Collection<T> decode(final BsonReader reader, final DecoderContext context) {
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

    private Collection<T> getInstance() {
        if (encoderClass.isInterface()) {
            if (encoderClass.isAssignableFrom(ArrayList.class)) {
                return new ArrayList<T>();
            } else if (encoderClass.isAssignableFrom(HashSet.class)) {
                return new HashSet<T>();
            } else {
                throw new CodecConfigurationException(format("Unsupported Collection interface of %s!", encoderClass.getName()));
            }
        }

        try {
            return encoderClass.getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            throw new CodecConfigurationException(e.getMessage(), e);
        }
    }
}
