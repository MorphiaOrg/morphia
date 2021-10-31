package dev.morphia.mapping.codec;

import com.mongodb.lang.Nullable;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Provider for codecs for Morphia entities
 *
 * @morphia.internal
 */
public class MorphiaCodecProvider implements CodecProvider {
    private final Map<Class<?>, Codec<?>> codecs = new HashMap<>();
    private final Mapper mapper;
    private final List<PropertyCodecProvider> propertyCodecProviders = new ArrayList<>();
    private final Datastore datastore;

    /**
     * Creates a provider
     *
     * @param datastore the Datastore to use
     */
    public MorphiaCodecProvider(Datastore datastore) {
        this.datastore = datastore;
        this.mapper = datastore.getMapper();

        propertyCodecProviders.addAll(List.of(new MorphiaMapPropertyCodecProvider(),
            new MorphiaCollectionPropertyCodecProvider()));

        ServiceLoader<MorphiaPropertyCodecProvider> providers = ServiceLoader.load(MorphiaPropertyCodecProvider.class);
        providers.forEach(propertyCodecProviders::add);
    }

    @Nullable
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
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

    protected Map<Class<?>, Codec<?>> getCodecs() {
        return codecs;
    }

    protected Datastore getDatastore() {
        return datastore;
    }

    protected List<PropertyCodecProvider> getPropertyCodecProviders() {
        return propertyCodecProviders;
    }

    /**
     * Creates a codec that uses an existing entity for loading rather than creating a new instance.
     *
     * @param entity   the entity to refresh
     * @param registry the codec registry
     * @param <T>      the entity type
     * @return the new codec
     */
    @Nullable
    public <T> Codec<T> getRefreshCodec(T entity, CodecRegistry registry) {
        EntityModel model = mapper.getEntityModel(entity.getClass());
        return new MorphiaCodec<>(datastore, model, propertyCodecProviders, mapper.getDiscriminatorLookup(), registry) {
            @Override
            protected EntityDecoder<T> getDecoder() {
                return new EntityDecoder(this) {
                    @Override
                    protected MorphiaInstanceCreator getInstanceCreator() {
                        return new MorphiaInstanceCreator() {
                            @Override
                            public T getInstance() {
                                return entity;
                            }

                            @Override
                            public void set(@Nullable Object value, PropertyModel model) {
                                model.getAccessor().set(entity, value);
                            }
                        };
                    }
                };
            }
        };
    }

    protected Mapper getMapper() {
        return mapper;
    }
}
