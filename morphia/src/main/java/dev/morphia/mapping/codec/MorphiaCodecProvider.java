package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.mapping.codec.pojo.MorphiaModel;
import dev.morphia.mapping.codec.pojo.MorphiaModelBuilder;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.DiscriminatorLookup;
import org.bson.codecs.pojo.PojoCodec;
import org.bson.codecs.pojo.PropertyCodecProvider;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provider for codecs for Morphia entities
 */
public class MorphiaCodecProvider implements CodecProvider {
    private final Map<Class<?>, PojoCodec<?>> codecs = new HashMap<>();
    private final Mapper mapper;
    private final List<Convention> conventions;
    private final DiscriminatorLookup discriminatorLookup;
    private final List<PropertyCodecProvider> propertyCodecProviders = new ArrayList<>();
    private final Datastore datastore;

    /**
     * Creates a provider
     *
     * @param mapper      the mapper to use
     * @param datastore   the datastore to use
     * @param packages    the packages to map
     * @param conventions the conventions to apply
     */
    public MorphiaCodecProvider(final Mapper mapper, final Datastore datastore, final Set<String> packages,
                                final List<Convention> conventions) {
        this.mapper = mapper;
        this.conventions = conventions;
        this.discriminatorLookup = new DiscriminatorLookup(new HashMap<>(), packages);
        propertyCodecProviders.add(new MorphiaMapPropertyCodecProvider());
        propertyCodecProviders.add(new MorphiaCollectionPropertyCodecProvider());
        this.datastore = datastore;
    }

    /**
     * Checks if a type is mappable or not
     *
     * @param type the class to check
     * @param <T>  the type
     * @return true if the type is mappable
     */
    public static <T> boolean isMappable(final Class<T> type) {
        final Class actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
        return hasAnnotation(actual, List.of(Entity.class, Embedded.class));
    }

    private static <T> boolean hasAnnotation(final Class<T> clazz, final List<Class<? extends Annotation>> annotations) {
        if (clazz == null) {
            return false;
        }
        for (Class<? extends Annotation> annotation : annotations) {
            if (clazz.getAnnotation(annotation) != null) {
                return true;
            }
        }

        return hasAnnotation(clazz.getSuperclass(), annotations)
               || Arrays.stream(clazz.getInterfaces())
                        .map(i -> hasAnnotation(i, annotations))
                        .reduce(false, (l, r) -> l || r);
    }

    private static <T> MorphiaModel<T> createMorphiaModel(final Datastore datastore,
                                                          final List<Convention> conventions, final Class<T> clazz) {
        MorphiaModelBuilder<T> builder = new MorphiaModelBuilder<>(datastore, clazz);
        if (conventions != null) {
            builder.conventions(conventions);
        }
        return builder.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(final Class<T> type, final CodecRegistry registry) {
        MorphiaCodec<T> codec = (MorphiaCodec<T>) codecs.get(type);
        if (codec == null && isMappable(type)) {
            MorphiaModel<T> morphiaModel = createMorphiaModel(datastore, conventions, type);
            discriminatorLookup.addClassModel(morphiaModel);
            codec = new MorphiaCodec<>(datastore, morphiaModel, registry, propertyCodecProviders, discriminatorLookup,
                new MappedClass(morphiaModel, mapper)
            );
            codecs.put(type, codec);
        }

        return codec;
    }

}
