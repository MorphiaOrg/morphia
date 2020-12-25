package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityDecoder;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
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
    public <T> MorphiaCodec get(Class<T> type, CodecRegistry registry) {
        MorphiaCodec codec = (MorphiaCodec) codecs.get(type);
        if (codec == null && (mapper.isMapped(type) || mapper.isMappable(type))) {
            codec = new MorphiaCodec(datastore, mapper.getEntityModel(type), propertyCodecProviders,
                mapper.getDiscriminatorLookup(), registry);
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
                    protected MorphiaInstanceCreator getInstanceCreator(EntityModel classModel) {
                        return new MorphiaInstanceCreator() {
                            @Override
                            public T getInstance() {
                                return entity;
                            }

                            @Override
                            public void set(Object value, FieldModel model) {
                                model.getAccessor().set(getInstance(), value);
                            }
                        };
                    }
                };
            }
        };
    }

}
