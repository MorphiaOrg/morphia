package dev.morphia.mapping.codec;

import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
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
 */
public class MorphiaCodecProvider implements CodecProvider {
    private final Map<Class<?>, Codec<?>> codecs = new HashMap<>();
    private final Mapper mapper;
    private final List<PropertyCodecProvider> propertyCodecProviders;
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

        propertyCodecProviders = List.of(new MorphiaMapPropertyCodecProvider(),
            new MorphiaCollectionPropertyCodecProvider());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MorphiaCodec<T> get(final Class<T> type, final CodecRegistry registry) {
        MorphiaCodec<T> codec = (MorphiaCodec<T>) codecs.get(type);
        if (codec == null && mapper.isMappable(type)) {
            codec = new MorphiaCodec<T>(datastore, mapper.getMappedClass(type), propertyCodecProviders,
                mapper.getDiscriminatorLookup(), registry);
            codecs.put(type, codec);
        }

        return codec;
    }

    @SuppressWarnings("unchecked")
    public <T> Codec<T> getRefreshCodec(final T entity, final CodecRegistry registry) {
        MorphiaCodec<T> codec = (MorphiaCodec<T>) get(entity.getClass(), registry);
        MongoCollection<?> collection = mapper.getCollection(entity.getClass());

        MappedClass mappedClass = mapper.getMappedClass(entity.getClass());
        return new MorphiaCodec<T>(datastore, mappedClass, propertyCodecProviders, mapper.getDiscriminatorLookup(), registry) {
            @Override
            protected EntityDecoder<T> getDecoder() {
                return new EntityDecoder<>(this) {
                    @Override
                    protected MorphiaInstanceCreator<T> getInstanceCreator(final EntityModel<T> classModel) {
                        return new MorphiaInstanceCreator<>() {
                            @Override
                            public T getInstance() {
                                return entity;
                            }

                            @Override
                            public <S> void set(final S value, final FieldModel<S> model) {
                                model.getAccessor().set(getInstance(), value);
                            }
                        };
                    }
                };
            }
        };
    }

}
