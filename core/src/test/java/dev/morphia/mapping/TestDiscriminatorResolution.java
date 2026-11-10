package dev.morphia.mapping;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.query.FindOptions;

import org.bson.Document;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Covers resolving stored discriminators to classes, and reading the discriminator under each model's own key.
 */
public class TestDiscriminatorResolution {
    private static final String UNKNOWN = "dev.morphia.mapping.NoSuchEntity";

    @Test
    public void unknownDiscriminatorsAreLookedUpOnce() {
        CountingLoader loader = new CountingLoader();
        DiscriminatorLookup lookup = new DiscriminatorLookup(loader);

        for (int i = 0; i < 3; i++) {
            Assertions.assertThrows(CodecConfigurationException.class, () -> lookup.lookup(UNKNOWN));
        }
        Assertions.assertEquals(List.of(UNKNOWN), loader.requested, "a miss should be remembered, not reloaded per document");
    }

    @Test
    public void classNameDiscriminatorsAreLookedUpOnce() {
        CountingLoader loader = new CountingLoader();
        DiscriminatorLookup lookup = new DiscriminatorLookup(loader);
        String name = Gadget.class.getName();

        for (int i = 0; i < 3; i++) {
            Assertions.assertSame(Gadget.class, lookup.lookup(name));
        }
        Assertions.assertEquals(List.of(name), loader.requested);
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void registeredDiscriminatorsDontLoadClasses(MapperType type) {
        Mapper mapper = mapper(type);
        String discriminator = mapper.getEntityModel(Gadget.class).discriminator();

        Assertions.assertSame(Gadget.class, mapper.getClass(discriminator));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void projectionsIncludeTheModelsDiscriminatorKey(MapperType type) {
        Mapper mapper = mapper(type);
        FindOptions options = new FindOptions().projection().include("name");

        Document projection = options.projection().map(mapper, Gadget.class);

        Assertions.assertEquals(new Document("name", 1).append("_kind", 1), projection);
    }

    private static Mapper mapper(MapperType type) {
        MorphiaConfig config = MorphiaConfig.load().mapper(type);
        return type == MapperType.CRITTER ? new CritterMapper(config) : new ReflectiveMapper(config);
    }

    /**
     * Records each class name the JVM asks it for.
     */
    private static class CountingLoader extends ClassLoader {
        private final List<String> requested = new CopyOnWriteArrayList<>();

        CountingLoader() {
            super(TestDiscriminatorResolution.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("dev.morphia.mapping.")) {
                requested.add(name);
            }
            return super.loadClass(name, resolve);
        }
    }

    @Entity(discriminatorKey = "_kind")
    static class Gadget {
        @Id
        private ObjectId id;
        private String name;
    }
}
