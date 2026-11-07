package dev.morphia.test.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.CritterMapper;
import dev.morphia.mapping.PropertyDiscovery;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

/**
 * Generic types are resolved against the entity being mapped, by type variable rather than by position, the same way
 * by both mappers.
 */
public class TestGenericResolution extends TestBase {

    @Test
    public void reorderedMapTypeParameters() {
        withTestConfig(List.of(HasReorderedMap.class), () -> {
            assertTypeData(HasReorderedMap.class, "map", "ReorderedMap<Integer, String>");

            HasReorderedMap entity = new HasReorderedMap();
            entity.map = new ReorderedMap<>();
            entity.map.put(1, "one");
            getDs().save(entity);

            HasReorderedMap loaded = getDs().find(HasReorderedMap.class).filter(eq("_id", entity.id)).first();
            Assertions.assertEquals(entity.map, loaded.map);
        });
    }

    @Test
    public void lowerBoundedWildcard() {
        withTestConfig(List.of(HasLowerBound.class), () -> {
            // a List<? super Integer> can hold any supertype's values, so only Object is safe to decode them as
            assertTypeData(HasLowerBound.class, "numbers", "List<? extends Object>");

            HasLowerBound entity = new HasLowerBound();
            entity.numbers = new ArrayList<Object>(List.of("text", 1));
            getDs().save(entity);

            HasLowerBound loaded = getDs().find(HasLowerBound.class).filter(eq("_id", entity.id)).first();
            Assertions.assertEquals(entity.numbers, loaded.numbers);
        });
    }

    @Test
    public void upperBoundedWildcard() {
        withTestConfig(List.of(HasUpperBound.class), () -> {
            assertTypeData(HasUpperBound.class, "numbers", "List<? extends Number>");
        });
    }

    @Test
    public void unboundGenericArray() {
        withTestConfig(List.of(ArrayBase.class), () -> {
            TypeData<?> typeData = getMapper().getEntityModel(ArrayBase.class).getProperty("array").getTypeData();
            Assertions.assertEquals(Object.class, typeData.getType());
            Assertions.assertTrue(typeData.isArray());
        });
    }

    @Test
    public void superclassBoundToParameterizedType() {
        withTestConfig(List.of(ListOfStrings.class), () -> {
            assertTypeData(ListOfStrings.class, "value", "List<String>");
            assertTypeData(ListOfStrings.class, "values", "List<List<String>>");
            assertTypeData(ListOfStrings.class, "valuesByName", "Map<String, List<String>>");

            ListOfStrings entity = new ListOfStrings();
            entity.value = List.of("a", "b");
            entity.values = List.of(List.of("c"));
            getDs().save(entity);

            ListOfStrings loaded = getDs().find(ListOfStrings.class).filter(eq("_id", entity.id)).first();
            Assertions.assertEquals(entity.value, loaded.value);
            Assertions.assertEquals(entity.values, loaded.values);
        });
    }

    @Test
    public void typeVariableReusedThroughTheHierarchy() {
        withTestConfig(List.of(StringLeaf.class), () -> {
            assertTypeData(StringLeaf.class, "value", "List<String>");
            assertTypeData(StringLeaf.class, "middle", "Integer");
        });
    }

    @Test
    public void genericArray() {
        withTestConfig(List.of(StringArray.class), () -> {
            assertTypeData(StringArray.class, "array", "String[]");
        });
    }

    @Test
    public void getterTypesResolved() {
        withTestConfig(buildConfig().propertyDiscovery(PropertyDiscovery.METHODS), List.of(MethodMappedStrings.class), () -> {
            assertTypeData(MethodMappedStrings.class, "value", "List<String>");
            assertTypeData(MethodMappedStrings.class, "values", "List<List<String>>");
        });
    }

    private void assertTypeData(Class<?> entity, String property, String expected) {
        EntityModel model = getMapper().getEntityModel(entity);
        // a fallback to reflection would test the reflective model twice
        if (getMapper() instanceof CritterMapper) {
            Assertions.assertInstanceOf(CritterEntityModel.class, model, entity.getName());
        }
        TypeData<?> typeData = model.getProperty(property).getTypeData();
        Assertions.assertEquals(expected, typeData.toString(), property);
    }

    public static class ReorderedMap<V, K> extends HashMap<K, V> {
    }

    @Entity
    public static class HasReorderedMap {
        @Id
        public ObjectId id;
        public ReorderedMap<String, Integer> map;
    }

    @Entity
    public static class HasLowerBound {
        @Id
        public ObjectId id;
        public List<? super Integer> numbers;
    }

    @Entity
    public static class HasUpperBound {
        @Id
        public ObjectId id;
        public List<? extends Number> numbers;
    }

    @Entity
    public static class Base<T> {
        @Id
        public ObjectId id;
        public T value;
        public List<T> values;
        public Map<String, T> valuesByName;
    }

    public static class ListOfStrings extends Base<List<String>> {
    }

    @Entity
    public static class Root<T> {
        @Id
        public ObjectId id;
        public T value;
    }

    public static class Middle<T, M> extends Root<List<T>> {
        public M middle;
    }

    public static class StringLeaf extends Middle<String, Integer> {
    }

    @Entity
    public static class ArrayBase<T> {
        @Id
        public ObjectId id;
        public T[] array;
    }

    public static class StringArray extends ArrayBase<String> {
    }

    @Entity
    public static class MethodMappedBase<T> {
        private ObjectId id;
        private T value;
        private List<T> values = new ArrayList<>();

        @Id
        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public List<T> getValues() {
            return values;
        }

        public void setValues(List<T> values) {
            this.values = values;
        }
    }

    public static class MethodMappedStrings extends MethodMappedBase<List<String>> {
    }
}
