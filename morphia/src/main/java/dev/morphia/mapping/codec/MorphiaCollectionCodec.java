package dev.morphia.mapping.codec;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class MorphiaCollectionCodec<T> extends CollectionCodec<T> {
    public MorphiaCollectionCodec(final TypeWithTypeParameters<T> type,
                                      final PropertyCodecRegistry registry,
                                      final TypeWithTypeParameters<T> valueType) {

        super((Class<Collection<T>>) type.getType(), registry.get(valueType));
    }

    @Override
    public void encode(final BsonWriter writer, final Collection<T> collection, final EncoderContext encoderContext) {
        super.encode(writer, collection, encoderContext);
    }

    @Override
    public Collection<T> decode(final BsonReader reader, final DecoderContext decoderContext) {
        if(reader.getCurrentBsonType().equals(BsonType.ARRAY)) {
            return super.decode(reader, decoderContext);
        }
        final Collection<T> collection = getInstance();
        T value = getCodec().decode(reader, decoderContext);
        collection.add(value);
        return collection;
    }

    private Collection<T> getInstance() {
        if (getEncoderClass().equals(Collection.class) || getEncoderClass().equals(List.class)) {
            return new ArrayList<>();
        } else if (getEncoderClass().equals(Set.class)) {
            return new HashSet<>();
        }
        try {
            final Constructor<Collection<T>> constructor = getEncoderClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (final Exception e) {
            throw new CodecConfigurationException(e.getMessage(), e);
        }
    }

}
