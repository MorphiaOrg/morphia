package dev.morphia.mapping;

import com.mongodb.BasicDBList;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.mapping.cache.DefaultEntityCache;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MappedFieldTest extends TestBase {
    @Test
    public void arrayFieldMapping() {
        final MappedField field = new MappedField(getField(TestEntity.class, "arrayOfInt"), TestEntity.class, getMorphia().getMapper());

        assertFalse(field.isSingleValue());
        assertTrue(field.isMultipleValues());
        assertTrue(field.isArray());
        assertTrue(field.getType().isArray());
        assertEquals("arrayOfInt", field.getJavaFieldName());
        assertEquals("arrayOfInt", field.getMappedFieldName());
    }

    @Test
    public void basicFieldMapping() {
        final MappedField field = new MappedField(getField(TestEntity.class, "name"), TestEntity.class, getMorphia().getMapper());

        assertTrue(field.isSingleValue());
        assertTrue(String.class == field.getType());
        assertEquals("name", field.getJavaFieldName());
        assertEquals("n", field.getMappedFieldName());
    }

    @Test
    public void collectionFieldMapping() {
        final MappedField field = new MappedField(getField(TestEntity.class, "listOfString"), TestEntity.class, getMorphia().getMapper());

        assertFalse(field.isSingleValue());
        assertTrue(field.isMultipleValues());
        assertFalse(field.isArray());
        assertTrue(List.class == field.getType());
        assertTrue(String.class == field.getSubType());
        assertEquals("listOfString", field.getJavaFieldName());
        assertEquals("listOfString", field.getMappedFieldName());
    }

    @Test
    public void idFieldMapping() {
        final MappedField field = new MappedField(getField(TestEntity.class, "id"), TestEntity.class, getMorphia().getMapper());

        assertTrue(field.isSingleValue());
        assertTrue(ObjectId.class == field.getType());
        assertEquals("id", field.getJavaFieldName());
        assertEquals("_id", field.getMappedFieldName());
    }

    @Test
    public void nestedCollectionsMapping() {
        final MappedField field = new MappedField(getField(TestEntity.class, "listOfListOfString"),
                                                  TestEntity.class,
                                                  getMorphia().getMapper());

        assertFalse(field.isSingleValue());
        assertTrue(field.isMultipleValues());
        assertFalse(field.isArray());
        assertTrue(List.class == field.getType());

        final List<MappedField> level1Types = field.getTypeParameters();
        final MappedField typeParameter = level1Types.get(0);
        assertTrue(List.class == typeParameter.getConcreteType());

        final List<MappedField> level2Types = typeParameter.getTypeParameters();
        final MappedField nested = level2Types.get(0);
        assertTrue(String.class == nested.getConcreteType());
        assertEquals("listOfListOfString", field.getJavaFieldName());
        assertEquals("listOfListOfString", field.getMappedFieldName());

        final BasicDBList list = new BasicDBList();
        list.add(dbList("a", "b", "c"));
        list.add(dbList("d", "e", "f"));
        final TestEntity entity = getMorphia().getMapper()
                                              .fromDocument(getDs(), TestEntity.class, new Document("listOfListOfString", list),
                                                  new DefaultEntityCache());
        final List<String> strings = asList("a", "b", "c");
        final List<String> strings1 = asList("d", "e", "f");
        final List<List<String>> expected = new ArrayList<>();
        expected.add(strings);
        expected.add(strings1);
        assertEquals(expected, entity.listOfListOfString);
    }

    private BasicDBList dbList(final String... values) {
        final BasicDBList list = new BasicDBList();
        Collections.addAll(list, values);
        return list;
    }

    private Field getField(final Class c, final String field) {
        try {
            return c.getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
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
