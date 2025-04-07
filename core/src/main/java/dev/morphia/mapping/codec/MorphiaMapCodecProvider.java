package dev.morphia.mapping.codec;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import dev.morphia.DatastoreImpl;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class MorphiaMapCodecProvider implements CodecProvider {
    private final DatastoreImpl datastore;

    public MorphiaMapCodecProvider(DatastoreImpl datastore) {
        this.datastore = datastore;
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, List<Type> typeArguments, CodecRegistry registry) {
        if (Map.class.isAssignableFrom(clazz) && !Document.class.isAssignableFrom(clazz)) {
            Class valueType = typeArguments.size() == 2 ? (Class) typeArguments.get(1) : Object.class;
            return (Codec<T>) new MorphiaMapCodec(datastore, clazz, registry.get(valueType));
        }
        return null;
    }
}
