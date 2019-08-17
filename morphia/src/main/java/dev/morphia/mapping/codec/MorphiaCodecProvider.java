package dev.morphia.mapping.codec;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.mapping.codec.pojo.MorphiaModel;
import dev.morphia.mapping.codec.pojo.MorphiaModelBuilder;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
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

public class MorphiaCodecProvider implements CodecProvider {
    private final Map<Class<?>, ClassModel<?>> classModels = new HashMap<>();
    private final Map<Class<?>, PojoCodec<?>> codecs = new HashMap<>();
    private final Mapper mapper;
    private final List<Convention> conventions;
    private final DiscriminatorLookup discriminatorLookup;
    private final List<PropertyCodecProvider> propertyCodecProviders = new ArrayList<>();

    public MorphiaCodecProvider(final Mapper mapper,
                                final List<Convention> conventions,
                                final Set<String> packages) {
        this.mapper = mapper;
        this.conventions = conventions;
        this.discriminatorLookup = new DiscriminatorLookup(this.classModels, packages);
        propertyCodecProviders.add(new MorphiaMapPropertyCodecProvider());
        propertyCodecProviders.add(new MorphiaCollectionPropertyCodecProvider());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(final Class<T> type, final CodecRegistry registry) {
        MorphiaCodec<T> codec = (MorphiaCodec<T>) codecs.get(type);
        if (codec == null && isMappable(type)) {
            MorphiaModel<T> morphiaModel = createMorphiaModel(type, conventions);
            discriminatorLookup.addClassModel(morphiaModel);
            codec = new MorphiaCodec<>(mapper, new MappedClass(morphiaModel, mapper), morphiaModel, registry,
                propertyCodecProviders, discriminatorLookup);
            codecs.put(type, codec);
        }

        return codec;
    }

    private <T> boolean isMappable(final Class<T> type) {
        return hasAnnotation(type, List.of(Entity.class, Embedded.class));
    }

    private <T> boolean hasAnnotation(final Class<T> clazz, final List<Class<? extends Annotation>> annotations) {
        if(clazz == null) {
            return false;
        }
        for (Class<? extends Annotation> annotation : annotations) {
            if(clazz.getAnnotation(annotation) != null) {
                return true;
            }
        }

        return hasAnnotation(clazz.getSuperclass(), annotations)
               || Arrays.stream(clazz.getInterfaces())
                             .map(i -> hasAnnotation(i, annotations))
                             .reduce(false, (l, r) -> l || r);
    }



    private static <T> MorphiaModel<T> createMorphiaModel(final Class<T> clazz, final List<Convention> conventions) {
        MorphiaModelBuilder<T> builder = new MorphiaModelBuilder<>(clazz);
        if (conventions != null) {
            builder.conventions(conventions);
        }
        return builder.build();
    }

}
