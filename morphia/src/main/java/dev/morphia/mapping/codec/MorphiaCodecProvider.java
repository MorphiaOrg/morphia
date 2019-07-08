package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.CollectionPropertyCodecProvider;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.DiscriminatorLookup;
import org.bson.codecs.pojo.MapPropertyCodecProvider;
import org.bson.codecs.pojo.PojoCodec;
import org.bson.codecs.pojo.PropertyCodecProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.bson.assertions.Assertions.notNull;

public class MorphiaCodecProvider implements CodecProvider {
    private final Map<Class<?>, ClassModel<?>> classModels = new HashMap<>();
    private final Map<Class<?>, PojoCodec<?>> codecs = new HashMap<>();
    private final Mapper mapper;
    private final List<Convention> conventions;
    private final DiscriminatorLookup discriminatorLookup;
    private final List<PropertyCodecProvider> propertyCodecProviders = new ArrayList<>();

    public MorphiaCodecProvider(final Datastore datastore,
                                final Mapper mapper,
                                final List<Convention> conventions,
                                final Set<String> packages) {
        this.mapper = mapper;
        this.conventions = conventions;
        this.discriminatorLookup = new DiscriminatorLookup(this.classModels, packages);
        propertyCodecProviders.add(new MapPropertyCodecProvider());
        propertyCodecProviders.add(new CollectionPropertyCodecProvider());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        if (mapper.isMappable(clazz)) {
            PojoCodec<T> codec = (PojoCodec<T>) codecs.get(clazz);
            if (codec == null) {
                ClassModel<T> classModel = createClassModel(clazz, conventions);
                discriminatorLookup.addClassModel(classModel);
                codec = new MorphiaCodec<>(mapper, new MappedClass(classModel, mapper), classModel, registry,
                    propertyCodecProviders, discriminatorLookup);
            }

            return (Codec<T>) codec;
        }
        return null;
    }

    /**
     * Registers codec providers that receive the type parameters of properties for instances encoded and decoded
     * by a {@link PojoCodec} handled by this provider.
     *
     * @param providers property codec providers to register
     */
    public void register(final PropertyCodecProvider... providers) {
        propertyCodecProviders.addAll(asList(notNull("providers", providers)));
    }

    private static <T> ClassModel<T> createClassModel(final Class<T> clazz, final List<Convention> conventions) {
        ClassModelBuilder<T> builder = ClassModel.builder(clazz);
        if (conventions != null) {
            builder.conventions(conventions);
        }
        return builder.build();
    }

}
