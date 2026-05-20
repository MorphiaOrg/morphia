package dev.morphia.test.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

public class TestPropertyModel extends TestBase {

    private EntityModel entityModel;

    @BeforeEach
    public void mapEntity() {
        entityModel = getMapper().getEntityModel(TestEntity.class);
    }

    @Test
    public void arrayFieldMapping() {
        final PropertyModel property = getMappedField("arrayOfInt");

        Assertions.assertFalse(property.isScalarValue());
        Assertions.assertTrue(property.isArray());
        Assertions.assertTrue(property.getType().isArray());
        Assertions.assertEquals("arrayOfInt", property.getName());
        Assertions.assertEquals("arrayOfInt", property.getMappedName());
    }

    @Test
    public void basicFieldMapping() {
        final PropertyModel property = getMappedField("name");

        Assertions.assertTrue(property.isScalarValue());
        Assertions.assertSame(String.class, property.getType());
        Assertions.assertEquals("name", property.getName());
        Assertions.assertEquals("n", property.getMappedName());
    }

    @Test
    public void collectionFieldMapping() {
        final PropertyModel property = getMappedField("listOfString");

        Assertions.assertFalse(property.isScalarValue());
        Assertions.assertFalse(property.isArray());
        Assertions.assertSame(List.class, property.getType());
        Assertions.assertSame(String.class, property.getNormalizedType());
        Assertions.assertEquals("listOfString", property.getName());
        Assertions.assertEquals("listOfString", property.getMappedName());
    }

    @Test
    public void idFieldMapping() {
        final PropertyModel property = getMappedField("id");

        Assertions.assertTrue(property.isScalarValue());
        Assertions.assertSame(ObjectId.class, property.getType());
        Assertions.assertEquals("id", property.getName());
        Assertions.assertEquals("_id", property.getMappedName());
    }

    @Test
    public void nestedCollectionsMapping() {
        final PropertyModel property = getMappedField("listOfListOfString");

        Assertions.assertFalse(property.isScalarValue());
        Assertions.assertFalse(property.isArray());
        Assertions.assertSame(List.class, property.getType());

        final TypeData<?> typeData = property.getTypeData();
        Assertions.assertSame(List.class, typeData.getType());

        Assertions.assertEquals(String.class, typeData.getTypeParameters().get(0).getTypeParameters().get(0).getType());
        Assertions.assertEquals("listOfListOfString", property.getName());
        Assertions.assertEquals("listOfListOfString", property.getMappedName());

        final List<List<String>> list = new ArrayList<>();
        list.add(dbList("a", "b", "c"));
        list.add(dbList("d", "e", "f"));
        TestEntity testEntity = new TestEntity();
        testEntity.listOfListOfString = list;
        getDs().save(testEntity);

        Assertions.assertEquals(getDs().find(TestEntity.class)
                .filter(eq("_id", testEntity.id))
                .first().listOfListOfString, list);
    }

    @Test
    public void nestedGenerics() {
        final PropertyModel property = getMappedField("nestedList");

        Assertions.assertFalse(property.isScalarValue());
        Assertions.assertFalse(property.isArray());
        Assertions.assertSame(List.class, property.getType());
        Assertions.assertSame(Nested.class, property.getNormalizedType());
    }

    private PropertyModel getMappedField(String name) {
        return entityModel.getProperty(name);
    }

    private List<String> dbList(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    @Entity(discriminator = "propTestEntity")
    private static class TestEntity {
        @Id
        private ObjectId id;
        @Property("n")
        private String name;
        private List<String> listOfString;
        private List<List<String>> listOfListOfString;
        private int[] arrayOfInt;
        private Map<String, Integer> mapOfInts;
        private List<Embed> listOfEmbeds;
        @Reference
        private List<Nested<String>> nestedList;
        private Map<String, Nested<String>> nestedMap;
    }

    @Entity
    private static class Embed {
        private String embedName;
        private List<Embed> embeddeds;
    }

    @Entity
    private static class Nested<K> {
        @Id
        private ObjectId id;
    }
}
