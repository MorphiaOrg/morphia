package dev.morphia.mapping;

import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import morphia.org.bson.codecs.pojo.TypeData;
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

public class FieldModelTest extends TestBase {

    private EntityModel entityModel;

    @Before
    public void mapping() {
        getMapper().map(List.of(TestEntity.class));
        entityModel = getMapper().getEntityModel(TestEntity.class);
    }

    @Test
    public void arrayFieldMapping() {
        final FieldModel field = getMappedField("arrayOfInt");

        assertFalse(field.isScalarValue());
        assertTrue(field.isMultipleValues());
        assertTrue(field.isArray());
        assertTrue(field.getType().isArray());
        assertEquals("arrayOfInt", field.getName());
        assertEquals("arrayOfInt", field.getMappedName());
    }

    private FieldModel getMappedField(String name) {
        return entityModel.getField(name);
    }

    @Test
    public void basicFieldMapping() {
        final FieldModel field = getMappedField("name");

        assertTrue(field.isScalarValue());
        assertSame(String.class, field.getType());
        assertEquals("name", field.getName());
        assertEquals("n", field.getMappedName());
    }

    @Test
    public void collectionFieldMapping() {
        final FieldModel field = getMappedField("listOfString");

        assertFalse(field.isScalarValue());
        assertTrue(field.isMultipleValues());
        assertFalse(field.isArray());
        assertSame(List.class, field.getType());
        assertSame(String.class, field.getNormalizedType());
        assertEquals("listOfString", field.getName());
        assertEquals("listOfString", field.getMappedName());
    }

    @Test
    public void idFieldMapping() {
        final FieldModel field = getMappedField("id");

        assertTrue(field.isScalarValue());
        assertSame(ObjectId.class, field.getType());
        assertEquals("id", field.getName());
        assertEquals("_id", field.getMappedName());
    }

    @Test
    public void nestedCollectionsMapping() {
        final FieldModel field = getMappedField("listOfListOfString");

        assertFalse(field.isScalarValue());
        assertTrue(field.isMultipleValues());
        assertFalse(field.isArray());
        assertSame(List.class, field.getType());

        final TypeData<?> typeData = field.getTypeData();
        assertSame(List.class, typeData.getType());

        assertEquals(String.class, typeData.getTypeParameters().get(0).getTypeParameters().get(0).getType());
        assertEquals("listOfListOfString", field.getName());
        assertEquals("listOfListOfString", field.getMappedName());

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

    @Embedded
    private static class Embed {
        private String embedName;
        private List<Embed> embeddeds;
    }
}
