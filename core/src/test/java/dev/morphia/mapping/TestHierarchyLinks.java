package dev.morphia.mapping;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.client.MongoCollection;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
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
    public void resolvesInheritedIndexFieldsAgainstTheIndexedModel(MapperType type) {
        MorphiaConfig config = MorphiaConfig.load().mapper(type).propertyDiscovery(PropertyDiscovery.METHODS);
        Mapper mapper = type == MapperType.CRITTER ? new CritterMapper(config) : new ReflectiveMapper(config);
        mapper.getEntityModel(MethodParent.class);
        List<String> created = new ArrayList<>();
        new IndexHelper(mapper).createIndex(recordingCollection(created), mapper.getEntityModel(MethodChild.class));

        Assertions.assertEquals(List.of("{\"child_name\": 1}"), created,
                "Parent's index on name should use the name MethodChild maps it to");
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void linksExternalEntitiesMappedChildFirst(MapperType type) {
        Mapper mapper = mapper(type);
        EntityModel child = mapper.getEntityModel(ExternalChildStandIn.class);
        EntityModel parent = mapper.getEntityModel(ExternalParentStandIn.class);

        Assertions.assertSame(parent, child.getSuperClass());
        Assertions.assertTrue(parent.getSubtypes().contains(child));
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
        Assertions.assertFalse(pending.contains(Serializable.class.getName()), "JDK types can never be mapped: " + pending);
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

    @Entity
    interface Tagged {
    }

    @Entity
    @Indexes(@Index(fields = @dev.morphia.annotations.Field("name")))
    static class Parent implements Serializable {
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
    @Indexes(@Index(fields = @dev.morphia.annotations.Field("name")))
    public static class MethodParent {
        private ObjectId id;
        private String name;

        @Id
        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        @Property("parent_name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity
    public static class MethodChild extends MethodParent {
        @Override
        @Property("child_name")
        public String getName() {
            return super.getName();
        }

        @Override
        public void setName(String name) {
            super.setName(name);
        }
    }

    public static class ExternalParent {
        ObjectId id;
    }

    public static class ExternalChild extends ExternalParent {
        ObjectId key;
        String name;
    }

    @ExternalEntity(target = ExternalParent.class, value = "external_parents")
    static class ExternalParentStandIn {
        @Id
        ObjectId id;
    }

    @ExternalEntity(target = ExternalChild.class, value = "external_parents")
    static class ExternalChildStandIn {
        @Id
        ObjectId key;
        String name;
    }

    @Entity
    static class Grandchild extends Child {
        String more;
    }
}
