package dev.morphia.mapping.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.CodecPlan;
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

/**
 * Provider for codecs for Morphia entities
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class MorphiaCodecProvider implements CodecProvider {
    // Looked up concurrently. Not computeIfAbsent: building a codec looks up other codecs, which may land back here.
    private final Map<Class<?>, Codec<?>> codecs = new ConcurrentHashMap<>();
    private final Mapper mapper;
    private final Conversions conversions;
    private final List<PropertyCodecProvider> propertyCodecProviders = new ArrayList<>();
    private MorphiaDatastore datastore;

    /**
     * Creates a provider
     *
     * @param datastore
     */
    public MorphiaCodecProvider(MorphiaDatastore datastore) {
        this.datastore = datastore;
        this.mapper = datastore.getMapper();
        this.conversions = mapper.getConversions();

        propertyCodecProviders.addAll(List.of(new MorphiaMapPropertyCodecProvider(datastore, conversions),
                new MorphiaCollectionPropertyCodecProvider()));

        ServiceLoader<MorphiaPropertyCodecProvider> providers = ServiceLoader.load(MorphiaPropertyCodecProvider.class);
        providers.forEach(provider -> {
            propertyCodecProviders.add(provider);
        });
    }

    @Nullable
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Codec<T> get(Class<T> type, CodecRegistry registry) {
        MorphiaCodec<T> codec = (MorphiaCodec<T>) codecs.get(type);
        if (codec == null && (mapper.isMapped(type) || mapper.isMappable(type))) {
            EntityModel model = mapper.getEntityModel(type);
            codec = new MorphiaCodec<>(datastore, model, propertyCodecProviders, mapper.getDiscriminatorLookup(), registry, conversions);
            // only the events the model or a global listener claims; a listener for one event leaves the others alone
            if (LifecycleEncoder.isNeeded(model, mapper)) {
                codec.setEncoder(new LifecycleEncoder(codec));
            }
            if (LifecycleDecoder.isNeeded(model, mapper)) {
                codec.setDecoder(new LifecycleDecoder(codec));
            }
            // another thread may have built one meanwhile; keep the first so every caller shares it
            MorphiaCodec<T> existing = (MorphiaCodec<T>) codecs.putIfAbsent(type, codec);
            if (existing != null) {
                codec = existing;
            }
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
    @Nullable
    public <T> Codec<T> getRefreshCodec(T entity, CodecRegistry registry) {
        EntityModel model = mapper.getEntityModel(entity.getClass());
        // decode with the entity's regular plan, so the property codecs this datastore already resolved are reused. a
        // subclass may provide some other codec for the entity; then the refresh codec builds a plan of its own
        CodecPlan plan = get(model.getType(), registry) instanceof MorphiaCodec<?> regular && regular.getPlan().model() == model
                ? regular.getPlan()
                : null;
        return new MorphiaCodec<>(datastore, model, propertyCodecProviders, mapper.getDiscriminatorLookup(), registry, conversions,
                plan) {
            @Override
            protected EntityDecoder<T> getDecoder() {
                return new EntityDecoder<>(this) {
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

                            @Override
                            public boolean isEagerInstanceSafe() {
                                return true;
                            }
                        };
                    }
                };
            }
        };
    }

    public Map<Class<?>, Codec<?>> codecs() {
        return codecs;
    }

    public MorphiaDatastore datastore() {
        return datastore;
    }

    public List<PropertyCodecProvider> propertyCodecProviders() {
        return propertyCodecProviders;
    }

    protected Mapper getMapper() {
        return mapper;
    }

    @Override
    public String toString() {
        return String.format("MorphiaCodecProvider{propertyCodecProviders=%s}", propertyCodecProviders);
    }
}
