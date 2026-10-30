package dev.morphia.mapping.codec.pojo;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.EntityModelImporter;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.references.ReferenceCodec;
import dev.morphia.test.mapping.codec.harness.models.HarnessReferences;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * A copy of a datastore, which is what every session is, shares its parent's mapper but builds its own codecs, so its
 * references fetch through the copy. The same holds for separate datastores built on one mapper.
 */
public class TestDatastoreCopies {
    @ParameterizedTest
    @EnumSource(MapperType.class)
    void copiesShareTheMapper(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = new MorphiaDatastore(client, config(mapperType));
            MorphiaDatastore copy = new MorphiaDatastore(datastore);

            Assertions.assertSame(datastore.getMapper(), copy.getMapper());
            Assertions.assertSame(datastore.getMapper().getConversions(), copy.getMapper().getConversions());
            Assertions.assertSame(datastore.getMapper().getEntityModel(HarnessReferences.class),
                    copy.getMapper().getEntityModel(HarnessReferences.class));
            Assertions.assertNotSame(datastore.getCodecRegistry().get(HarnessReferences.class),
                    copy.getCodecRegistry().get(HarnessReferences.class));
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void referencesAreBoundToTheirOwnDatastore(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaConfig config = config(mapperType);
            MorphiaDatastore datastore = new MorphiaDatastore(client, config);
            // the parent's codecs are built first, as they would be in an application
            assertReferencesBoundTo(datastore);

            assertReferencesBoundTo(new MorphiaDatastore(datastore));
            assertReferencesBoundTo(new MorphiaDatastore(client, config, datastore.getMapper()));
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void copiesKeepTheImportersCodecProviders(MapperType mapperType, @TempDir Path services) throws IOException {
        Path file = services.resolve("META-INF/services/" + EntityModelImporter.class.getName());
        Files.createDirectories(file.getParent());
        Files.writeString(file, RecordingImporter.class.getName());

        RecordingImporter.datastores.clear();
        RecordingImporter.instances.clear();
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try (MongoClient client = MongoClients.create("mongodb://localhost:1");
                URLClassLoader loader = new URLClassLoader(new URL[] { services.toUri().toURL() }, original)) {
            thread.setContextClassLoader(loader);
            MorphiaDatastore datastore = new MorphiaDatastore(client, config(mapperType));
            thread.setContextClassLoader(original);
            MorphiaDatastore copy = new MorphiaDatastore(datastore);

            Assertions.assertEquals(List.of(datastore, copy), RecordingImporter.datastores);
            Assertions.assertEquals(1, RecordingImporter.instances.size(), "the importers are loaded once, by the parent");
            Assertions.assertTrue(copy.morphiaCodecProviders.stream()
                    .anyMatch(provider -> provider instanceof RecordingProvider && provider.datastore() == copy));
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    private static void assertReferencesBoundTo(MorphiaDatastore datastore) {
        CodecPlan plan = ((MorphiaCodec<?>) datastore.getCodecRegistry().get(HarnessReferences.class)).getPlan();
        int references = 0;
        for (PropertyModel property : plan.model().getProperties()) {
            if (plan.codec(property) instanceof ReferenceCodec codec) {
                references++;
                Assertions.assertSame(datastore, codec.getDatastore(), property.getName());
                Assertions.assertSame(codec, datastore.getPropertyCodec(property), property.getName());
            }
        }
        Assertions.assertEquals(3, references);
    }

    private static MorphiaConfig config(MapperType mapperType) {
        return ManualMorphiaConfig.configure()
                .database("datastore_copies")
                .mapper(mapperType)
                .packages(List.of(HarnessReferences.class.getPackageName()))
                .applyIndexes(false)
                .applyCaps(false)
                .applyDocumentValidations(false);
    }

    /**
     * Registers no models; records which datastores ask it for a codec provider.
     */
    public static class RecordingImporter implements EntityModelImporter {
        static final List<MorphiaDatastore> datastores = new ArrayList<>();
        static final List<RecordingImporter> instances = new ArrayList<>();

        public RecordingImporter() {
            instances.add(this);
        }

        @Override
        public MorphiaCodecProvider getCodecProvider(MorphiaDatastore datastore) {
            datastores.add(datastore);
            return new RecordingProvider(datastore);
        }

        @Override
        public List<EntityModel> getModels(Mapper mapper) {
            return List.of();
        }
    }

    /**
     * Provides no codecs.
     */
    static class RecordingProvider extends MorphiaCodecProvider {
        RecordingProvider(MorphiaDatastore datastore) {
            super(datastore);
        }

        @Override
        public <T> org.bson.codecs.Codec<T> get(Class<T> type, org.bson.codecs.configuration.CodecRegistry registry) {
            return null;
        }

        @Override
        public <T> org.bson.codecs.Codec<T> getRefreshCodec(T entity, org.bson.codecs.configuration.CodecRegistry registry) {
            return null;
        }
    }
}
