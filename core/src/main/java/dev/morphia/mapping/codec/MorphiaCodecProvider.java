package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityDecoder;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.LifecycleDecoder;
import dev.morphia.mapping.codec.pojo.LifecycleEncoder;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PropertyCodecProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for codecs for Morphia entities
 *
 * @morphia.internal
 */
public class MorphiaCodecProvider implements CodecProvider {
    private final Map<Class<?>, Codec<?>> codecs = new HashMap<>();
    private final Mapper mapper;
    private final List<PropertyCodecProvider> propertyCodecProviders;
    private final Datastore datastore;

    /**
     * Creates a provider
     *
     * @param mapper    the mapper to use
     * @param datastore the datastore to use
     */
    public MorphiaCodecProvider(Mapper mapper, Datastore datastore) {
        this.datastore = datastore;
        this.mapper = mapper;

        propertyCodecProviders = List.of(new MorphiaMapPropertyCodecProvider(),
            new MorphiaCollectionPropertyCodecProvider());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> type, CodecRegistry registry) {
        MorphiaCodec<T> codec = (MorphiaCodec<T>) codecs.get(type);
        if (codec == null && (mapper.isMapped(type) || mapper.isMappable(type))) {
            EntityModel model = mapper.getEntityModel(type);
            codec = new MorphiaCodec<>(datastore, model, propertyCodecProviders, mapper.getDiscriminatorLookup(), registry);
            if (model.hasLifecycle(PostPersist.class) || model.hasLifecycle(PrePersist.class) || mapper.hasInterceptors()) {
                codec.setEncoder(new LifecycleEncoder(codec));
            }
            if (model.hasLifecycle(PreLoad.class) || model.hasLifecycle(PostLoad.class) || mapper.hasInterceptors()) {
                codec.setDecoder(new LifecycleDecoder(codec));
            }
            codecs.put(type, codec);
        }

        return codec;
    }

    /**
     * Creates a codec that uses an existing entity for loading rather than creating a new instance.
     *
     * @param entity   the entity to refresh
     * @param registry the codec registry
     * @param <T>      the entity type
     * @return the new codec
     */
    public <T> Codec<T> getRefreshCodec(T entity, CodecRegistry registry) {
        EntityModel model = mapper.getEntityModel(entity.getClass());
        return new MorphiaCodec<>(datastore, model, propertyCodecProviders, mapper.getDiscriminatorLookup(), registry) {
            @Override
            protected EntityDecoder getDecoder() {
                return new EntityDecoder(this) {
                    @Override
                    protected MorphiaInstanceCreator getInstanceCreator() {
                        return new MorphiaInstanceCreator() {
                            @Override
                            public T getInstance() {
                                return entity;
                            }

                            @Override
                            public void set(Object value, PropertyModel model) {
                                model.getAccessor().set(entity, value);
                            }
                        };
                    }
                };
            }
        };
    }

}
