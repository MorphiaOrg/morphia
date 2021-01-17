package dev.morphia.mapping;

import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PropertyModelTest extends TestBase {

    private EntityModel entityModel;

    @Before
    public void mapping() {
        getMapper().map(List.of(TestEntity.class));
        entityModel = getMapper().getEntityModel(TestEntity.class);
    }

    @Test
    public void arrayFieldMapping() {
        final PropertyModel property = getMappedField("arrayOfInt");

        assertFalse(property.isScalarValue());
        assertTrue(property.isMultipleValues());
        assertTrue(property.isArray());
        assertTrue(property.getType().isArray());
        assertEquals("arrayOfInt", property.getName());
        assertEquals("arrayOfInt", property.getMappedName());
    }

    @Test
    public void basicFieldMapping() {
        final PropertyModel property = getMappedField("name");

        assertTrue(property.isScalarValue());
        assertSame(String.class, property.getType());
        assertEquals("name", property.getName());
        assertEquals("n", property.getMappedName());
    }

    @Test
    public void collectionFieldMapping() {
        final PropertyModel property = getMappedField("listOfString");

        assertFalse(property.isScalarValue());
        assertTrue(property.isMultipleValues());
        assertFalse(property.isArray());
        assertSame(List.class, property.getType());
        assertSame(String.class, property.getNormalizedType());
        assertEquals("listOfString", property.getName());
        assertEquals("listOfString", property.getMappedName());
    }

    @Test
    public void idFieldMapping() {
        final PropertyModel property = getMappedField("id");

        assertTrue(property.isScalarValue());
        assertSame(ObjectId.class, property.getType());
        assertEquals("id", property.getName());
        assertEquals("_id", property.getMappedName());
    }

    @Test
    public void nestedCollectionsMapping() {
        final PropertyModel property = getMappedField("listOfListOfString");

        assertFalse(property.isScalarValue());
        assertTrue(property.isMultipleValues());
        assertFalse(property.isArray());
        assertSame(List.class, property.getType());

        final TypeData<?> typeData = property.getTypeData();
        assertSame(List.class, typeData.getType());

        assertEquals(String.class, typeData.getTypeParameters().get(0).getTypeParameters().get(0).getType());
        assertEquals("listOfListOfString", property.getName());
        assertEquals("listOfListOfString", property.getMappedName());

        final List<List<String>> list = new ArrayList<>();
        list.add(dbList("a", "b", "c"));
        list.add(dbList("d", "e", "f"));
        TestEntity testEntity = new TestEntity();
        testEntity.listOfListOfString = list;
        getDs().save(testEntity);

        assertEquals(list, getDs().find(TestEntity.class)
                                  .filter(eq("_id", testEntity.id))
                                  .first()
                               .listOfListOfString);
    }

    private PropertyModel getMappedField(String name) {
        return entityModel.getProperty(name);
    }

    private List<String> dbList(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    @Entity
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
    }

    @Entity
    private static class Embed {
        private String embedName;
        private List<Embed> embeddeds;
    }
}
