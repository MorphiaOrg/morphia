package dev.morphia.mapping;

import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import org.bson.codecs.pojo.TypeData;
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
import static org.junit.Assert.assertTrue;

public class MappedFieldTest extends TestBase {

    private MappedClass mappedClass;

    @Before
    public void mapping() {
        getMapper().map(List.of(TestEntity.class));
        mappedClass = getMapper().getMappedClass(TestEntity.class);
    }
    @Test
    public void arrayFieldMapping() {
        final MappedField field = getMappedField("arrayOfInt");

        assertFalse(field.isScalarValue());
        assertTrue(field.isMultipleValues());
        assertTrue(field.isArray());
        assertTrue(field.getType().isArray());
        assertEquals("arrayOfInt", field.getJavaFieldName());
        assertEquals("arrayOfInt", field.getMappedFieldName());
    }

    private MappedField getMappedField(final String name) {
        return mappedClass.getMappedField(name);
    }

    @Test
    public void basicFieldMapping() {
        final MappedField field = getMappedField("name");

        assertTrue(field.isScalarValue());
        assertTrue(String.class == field.getType());
        assertEquals("name", field.getJavaFieldName());
        assertEquals("n", field.getMappedFieldName());
    }

    @Test
    public void collectionFieldMapping() {
        final MappedField field = getMappedField("listOfString");

        assertFalse(field.isScalarValue());
        assertTrue(field.isMultipleValues());
        assertFalse(field.isArray());
        assertTrue(List.class == field.getType());
        assertTrue(String.class == field.getNormalizedType());
        assertEquals("listOfString", field.getJavaFieldName());
        assertEquals("listOfString", field.getMappedFieldName());
    }

    @Test
    public void idFieldMapping() {
        final MappedField field = getMappedField("id");

        assertTrue(field.isScalarValue());
        assertTrue(ObjectId.class == field.getType());
        assertEquals("id", field.getJavaFieldName());
        assertEquals("_id", field.getMappedFieldName());
    }

    @Test
    public void nestedCollectionsMapping() {
        final MappedField field = getMappedField("listOfListOfString");

        assertFalse(field.isScalarValue());
        assertTrue(field.isMultipleValues());
        assertFalse(field.isArray());
        assertTrue(List.class == field.getType());

        final TypeData<?> typeData = field.getTypeData();
        final Class<?> typeParameter = typeData.getType();
        assertTrue(List.class == typeData.getType());

        assertEquals(String.class, typeData.getTypeParameters().get(0).getTypeParameters().get(0).getType());
        assertEquals("listOfListOfString", field.getJavaFieldName());
        assertEquals("listOfListOfString", field.getMappedFieldName());

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

    private List<String> dbList(final String... values) {
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
