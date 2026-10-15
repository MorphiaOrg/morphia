package dev.morphia.mapping;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.Duration;
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
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

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

    /**
     * Looks up a class's model and, under CRITTER, asserts it's a generated model; {@link CritterMapper} silently falls
     * back to a reflective model when generation fails, which would hide critter-only regressions.
     */
    private static EntityModel model(Mapper mapper, MapperType type, Class<?> entity) {
        EntityModel model = mapper.getEntityModel(entity);
        if (type == MapperType.CRITTER) {
            Assertions.assertInstanceOf(CritterEntityModel.class, model,
                    "Expected a critter model for " + entity.getName() + " but got " + model.getClass().getName());
        }
        return model;
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void subtypesOfChainedInterfacesAreFoundOnce(MapperType type) {
        Mapper mapper = mapper(type);
        EntityModel implementation = model(mapper, type, ChainImplementation.class);
        for (Class<?> link = Chain24.class; link != null; link = link.getInterfaces().length == 0 ? null : link.getInterfaces()[0]) {
            mapper.getEntityModel(link);
        }

        // each link sits under every link above it, so without a visited set this walk is exponential in the chain length
        Set<EntityModel> subtypes = Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> mapper.getEntityModel(Chain01.class).getSubtypes());
        Assertions.assertTrue(subtypes.contains(implementation));
        Assertions.assertEquals(24, subtypes.size(), "the 23 other links and the implementation");
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void superClassIsTheSuperclassNotAnInterface(MapperType type) {
        Mapper mapper = mapper(type);
        EntityModel child = model(mapper, type, Child.class);
        EntityModel parent = model(mapper, type, Parent.class);
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
        EntityModel parent = model(mapper, type, Parent.class);
        EntityModel tagged = mapper.getEntityModel(Tagged.class);
        EntityModel child = model(mapper, type, Child.class);

        Assertions.assertSame(parent, child.getSuperClass());
        Assertions.assertTrue(parent.getSubtypes().contains(child));
        Assertions.assertTrue(tagged.getSubtypes().contains(child));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void inheritedInterfacesAreLinked(MapperType type) {
        Mapper mapper = mapper(type);
        EntityModel grandchild = model(mapper, type, Grandchild.class);
        EntityModel tagged = mapper.getEntityModel(Tagged.class);

        Assertions.assertTrue(tagged.getSubtypes().contains(grandchild),
                "Grandchild implements Tagged through Child");
        Assertions.assertSame(model(mapper, type, Child.class), grandchild.getSuperClass());
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void inheritsTheSuperclassIndexes(MapperType type) {
        Mapper mapper = mapper(type);
        model(mapper, type, Parent.class);
        List<String> created = new ArrayList<>();
        new IndexHelper(mapper).createIndex(recordingCollection(created), model(mapper, type, Child.class));

        Assertions.assertEquals(List.of("{\"name\": 1}"), created, "Child should inherit Parent's @Indexes");
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void combinesDeclaredAndSuperclassIndexes(MapperType type) {
        Mapper mapper = mapper(type);
        model(mapper, type, Parent.class);
        List<String> created = new ArrayList<>();
        new IndexHelper(mapper).createIndex(recordingCollection(created), model(mapper, type, IndexedChild.class));

        Assertions.assertEquals(List.of("{\"extra\": 1}", "{\"name\": 1}"), created);
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void resolvesInheritedIndexFieldsAgainstTheIndexedModel(MapperType type) {
        MorphiaConfig config = MorphiaConfig.load().mapper(type).propertyDiscovery(PropertyDiscovery.METHODS);
        Mapper mapper = type == MapperType.CRITTER ? new CritterMapper(config) : new ReflectiveMapper(config);
        model(mapper, type, MethodParent.class);
        List<String> created = new ArrayList<>();
        new IndexHelper(mapper).createIndex(recordingCollection(created), model(mapper, type, MethodChild.class));

        Assertions.assertEquals(List.of("{\"child_name\": 1}"), created,
                "Parent's index on name should use the name MethodChild maps it to");
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void linksExternalEntitiesMappedChildFirst(MapperType type) {
        Mapper mapper = mapper(type);
        EntityModel child = model(mapper, type, ExternalChildStandIn.class);
        EntityModel parent = model(mapper, type, ExternalParentStandIn.class);

        Assertions.assertSame(parent, child.getSuperClass());
        Assertions.assertTrue(parent.getSubtypes().contains(child));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    @SuppressWarnings("unchecked")
    public void unmappableParentsArentPending(MapperType type) throws ReflectiveOperationException {
        Mapper mapper = mapper(type);
        model(mapper, type, Child.class);

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
    interface Chain01 {
    }

    @Entity
    interface Chain02 extends Chain01 {
    }

    @Entity
    interface Chain03 extends Chain02 {
    }

    @Entity
    interface Chain04 extends Chain03 {
    }

    @Entity
    interface Chain05 extends Chain04 {
    }

    @Entity
    interface Chain06 extends Chain05 {
    }

    @Entity
    interface Chain07 extends Chain06 {
    }

    @Entity
    interface Chain08 extends Chain07 {
    }

    @Entity
    interface Chain09 extends Chain08 {
    }

    @Entity
    interface Chain10 extends Chain09 {
    }

    @Entity
    interface Chain11 extends Chain10 {
    }

    @Entity
    interface Chain12 extends Chain11 {
    }

    @Entity
    interface Chain13 extends Chain12 {
    }

    @Entity
    interface Chain14 extends Chain13 {
    }

    @Entity
    interface Chain15 extends Chain14 {
    }

    @Entity
    interface Chain16 extends Chain15 {
    }

    @Entity
    interface Chain17 extends Chain16 {
    }

    @Entity
    interface Chain18 extends Chain17 {
    }

    @Entity
    interface Chain19 extends Chain18 {
    }

    @Entity
    interface Chain20 extends Chain19 {
    }

    @Entity
    interface Chain21 extends Chain20 {
    }

    @Entity
    interface Chain22 extends Chain21 {
    }

    @Entity
    interface Chain23 extends Chain22 {
    }

    @Entity
    interface Chain24 extends Chain23 {
    }

    @Entity
    static class ChainImplementation implements Chain24 {
        @Id
        ObjectId id;
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
