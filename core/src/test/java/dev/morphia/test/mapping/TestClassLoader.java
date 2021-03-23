package dev.morphia.test.mapping;

import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.test.TestBase;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.jar.asm.Opcodes;
import org.bson.Document;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.function.Function;

import static org.testng.Assert.assertTrue;

public class TestClassLoader extends TestBase {
    @Test(expectedExceptions = CodecConfigurationException.class)
    public void testNotUsingClassLoader() {
        useClassLoading(cl -> getMapper().getCollection(BasicEntity.class));
    }

    @Test
    public void testUsingClassLoader() {
        useClassLoading(this::recreateCollection);
    }

    private Class<?> loadDynamicClass(ClassLoader classLoader) {
        return new ByteBuddy().subclass(Base.class)
                              .name("dev.morphia.test.mapping.ChildEmbed")
                              .defineField("type", String.class, Opcodes.ACC_PUBLIC)
                              .annotateType(Base.class.getAnnotation(Entity.class))
                              .make()
                              .load(classLoader)
                              .getLoaded();
    }

    private MongoCollection<BasicEntity> recreateCollection(ClassLoader classLoader) {
        MapperOptions options = MapperOptions.builder()
                                             .discriminator(DiscriminatorFunction.className())
                                             .classLoader(classLoader)
                                             .build();
        Datastore datastore = Morphia.createDatastore(getMongoClient(), TEST_DB_NAME, options);
        return datastore.getMapper().getCollection(BasicEntity.class);
    }

    private void storePreviousInstance() {
        Document data = new Document("_t", "dev.morphia.test.mapping.ChildEmbed");
        data.put("type", "one");
        getMapper()
            .getCollection(BasicEntity.class)
            .withDocumentClass(Document.class)
            .insertOne(new Document("data", data));
    }

    private void useClassLoading(Function<ClassLoader, MongoCollection<BasicEntity>> collectionCreator) {
        storePreviousInstance();
        ClassLoader classLoader = new ByteArrayClassLoader(new AppClassLoader(), false, Map.of());
        Class<?> childClass = loadDynamicClass(classLoader);

        BasicEntity res = collectionCreator.apply(classLoader).find().first();
        assertTrue(childClass.isInstance(res.data));
    }

    private static class AppClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return super.loadClass(name);
        }
    }

    @Entity
    public abstract static class Base {
    }

    @Entity(useDiscriminator = false)
    static class BasicEntity {
        @Id
        ObjectId id;
        Base data;
    }
}
