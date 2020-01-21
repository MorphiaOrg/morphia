package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PropertyCodecProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for codecs for Morphia entities
 */
public class MorphiaCodecProvider implements CodecProvider {
    private final Map<Class<?>, Codec<?>> codecs = new HashMap<>();
    private final Mapper mapper;
    private final List<PropertyCodecProvider> propertyCodecProviders = new ArrayList<>();
    private final Datastore datastore;

    /**
     * Creates a provider
     *
     * @param mapper      the mapper to use
     * @param datastore   the datastore to use
     */
    public MorphiaCodecProvider(final Mapper mapper, final Datastore datastore) {
        this.datastore = datastore;
        this.mapper = mapper;

        propertyCodecProviders.add(new MorphiaMapPropertyCodecProvider());
        propertyCodecProviders.add(new MorphiaCollectionPropertyCodecProvider());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(final Class<T> type, final CodecRegistry registry) {
        MorphiaCodec<T> codec = (MorphiaCodec<T>) codecs.get(type);
        if (codec == null && mapper.isMappable(type)) {
            codec = new MorphiaCodec<T>(datastore, mapper.getMappedClass(type), propertyCodecProviders,
                mapper.getDiscriminatorLookup(),  registry);
            codecs.put(type, codec);
        }

        return codec;
    }

}
