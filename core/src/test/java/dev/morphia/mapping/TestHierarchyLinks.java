package dev.morphia.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.client.MongoCollection;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.internal.IndexHelper;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.pojo.EntityModel;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Covers how the mappers link a model to its superclass and interfaces.
 */
public class TestHierarchyLinks {
    private static Mapper mapper(MapperType type) {
        MorphiaConfig config = MorphiaConfig.load().mapper(type);
        return type == MapperType.CRITTER ? new CritterMapper(config) : new ReflectiveMapper(config);
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void superClassIsTheSuperclassNotAnInterface(MapperType type) {
        Mapper mapper = mapper(type);
        EntityModel child = mapper.getEntityModel(Child.class);
        EntityModel parent = mapper.getEntityModel(Parent.class);
        EntityModel tagged = mapper.getEntityModel(Tagged.class);

        Assertions.assertSame(parent, child.getSuperClass());
        Assertions.assertNull(parent.getSuperClass(), "Object isn't mapped, so Parent has no superclass model");
        Assertions.assertNull(tagged.getSuperClass());
        Assertions.assertTrue(parent.getSubtypes().contains(child));
        Assertions.assertTrue(tagged.getSubtypes().contains(child));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void superClassIsTheSuperclassWhenTheParentIsMappedFirst(MapperType type) {
        Mapper mapper = mapper(type);
        EntityModel parent = mapper.getEntityModel(Parent.class);
        EntityModel tagged = mapper.getEntityModel(Tagged.class);
        EntityModel child = mapper.getEntityModel(Child.class);

        Assertions.assertSame(parent, child.getSuperClass());
        Assertions.assertTrue(parent.getSubtypes().contains(child));
        Assertions.assertTrue(tagged.getSubtypes().contains(child));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void inheritedInterfacesAreLinked(MapperType type) {
        Mapper mapper = mapper(type);
        EntityModel grandchild = mapper.getEntityModel(Grandchild.class);
        EntityModel tagged = mapper.getEntityModel(Tagged.class);

        Assertions.assertTrue(tagged.getSubtypes().contains(grandchild),
                "Grandchild implements Tagged through Child");
        Assertions.assertSame(mapper.getEntityModel(Child.class), grandchild.getSuperClass());
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void inheritsTheSuperclassIndexes(MapperType type) {
        Mapper mapper = mapper(type);
        mapper.getEntityModel(Parent.class);
        List<String> created = new ArrayList<>();
        new IndexHelper(mapper).createIndex(recordingCollection(created), mapper.getEntityModel(Child.class));

        Assertions.assertEquals(List.of("{\"name\": 1}"), created, "Child should inherit Parent's @Indexes");
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void combinesDeclaredAndSuperclassIndexes(MapperType type) {
        Mapper mapper = mapper(type);
        mapper.getEntityModel(Parent.class);
        List<String> created = new ArrayList<>();
        new IndexHelper(mapper).createIndex(recordingCollection(created), mapper.getEntityModel(IndexedChild.class));

        Assertions.assertEquals(List.of("{\"extra\": 1}", "{\"name\": 1}"), created);
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    @SuppressWarnings("unchecked")
    public void unmappableParentsArentPending(MapperType type) throws ReflectiveOperationException {
        Mapper mapper = mapper(type);
        mapper.getEntityModel(Child.class);

        Field field = AbstractMapper.class.getDeclaredField("pendingSubtypesByParentName");
        field.setAccessible(true);
        Set<String> pending = ((Map<String, ?>) field.get(mapper)).keySet();
        Assertions.assertFalse(pending.contains(Object.class.getName()), "Object can never be mapped: " + pending);
        Assertions.assertFalse(pending.contains(Unmapped.class.getName()), "Unmapped can never be mapped: " + pending);
    }

    @SuppressWarnings("unchecked")
    private static MongoCollection<?> recordingCollection(List<String> created) {
        return (MongoCollection<?>) Proxy.newProxyInstance(TestHierarchyLinks.class.getClassLoader(),
                new Class<?>[] { MongoCollection.class }, (proxy, method, args) -> {
                    if (method.getName().equals("createIndex")) {
                        created.add(((Bson) args[0]).toBsonDocument().toJson());
                        return "index";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    interface Unmapped {
    }

    @Entity
    interface Tagged {
    }

    @Entity
    @Indexes(@Index(fields = @dev.morphia.annotations.Field("name")))
    static class Parent implements Unmapped {
        @Id
        ObjectId id;
        String name;
    }

    @Entity
    static class Child extends Parent implements Tagged {
        String extra;
    }

    @Entity
    @Indexes(@Index(fields = @dev.morphia.annotations.Field("extra")))
    static class IndexedChild extends Parent {
        String extra;
    }

    @Entity
    static class Grandchild extends Child {
        String more;
    }
}
