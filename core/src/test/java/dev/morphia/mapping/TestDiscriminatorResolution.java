package dev.morphia.mapping;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.query.FindOptions;
import dev.morphia.test.models.external.ThirdPartyEntity;
import dev.morphia.test.models.external.ThirdPartyEntityProxy;

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

    /**
     * A discriminator stored under an entity's own key resolves only to that entity, and only under that key. Under the
     * configured key, lookup works as it always has.
     */
    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void discriminatorsResolveUnderTheirOwnKey(MapperType type) {
        Mapper mapper = mapper(type);
        String gadget = mapper.getEntityModel(Gadget.class).discriminator();
        String plain = mapper.getEntityModel(Undiscriminated.class).discriminator();
        DiscriminatorLookup lookup = mapper.getDiscriminatorLookup();

        Assertions.assertTrue(lookup.isKey("_t", "_kind"));
        Assertions.assertTrue(lookup.isKey("_t", "_t"));
        Assertions.assertFalse(lookup.isKey("_t", "name"));
        Assertions.assertSame(Gadget.class, lookup.lookup("_t", "_kind", gadget));
        Assertions.assertSame(Gadget.class, lookup.lookup("_t", "_t", gadget), "the configured key, as before");
        Assertions.assertNull(lookup.lookup("_t", "_kind", "no such entity"), "not a discriminator: a plain field");
        Assertions.assertNull(lookup.lookup("_t", "_kind", plain), "the discriminator of an entity that doesn't store one");
        Assertions.assertThrows(CodecConfigurationException.class, () -> lookup.lookup("_t", "_t", UNKNOWN));
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

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void externalEntityProjectionsIncludeTheirDiscriminatorKey(MapperType type) {
        Mapper mapper = mapper(type);
        mapper.getEntityModel(ThirdPartyEntityProxy.class);
        FindOptions options = new FindOptions().projection().include("field");

        Assertions.assertEquals(new Document("field", 1).append("_xt", 1), options.projection().map(mapper, ThirdPartyEntity.class));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void projectionsWithoutADiscriminatorAddNone(MapperType type) {
        Mapper mapper = mapper(type);
        FindOptions options = new FindOptions().projection().include("name");

        Assertions.assertEquals(new Document("name", 1), options.projection().map(mapper, Undiscriminated.class));
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

    @Entity(useDiscriminator = false)
    static class Undiscriminated {
        @Id
        private ObjectId id;
        private String name;
    }

    @Entity(discriminatorKey = "_kind")
    static class Gadget {
        @Id
        private ObjectId id;
        private String name;
    }
}
