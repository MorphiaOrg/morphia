package dev.morphia.test.mapping;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static dev.morphia.query.experimental.filters.Filters.eq;

public class TestPropertyModel extends TestBase {

    private EntityModel entityModel;

    @Test
    public void arrayFieldMapping() {
        final PropertyModel property = getMappedField("arrayOfInt");

        Assert.assertFalse(property.isScalarValue());
        Assert.assertTrue(property.isMultipleValues());
        Assert.assertTrue(property.isArray());
        Assert.assertTrue(property.getType().isArray());
        Assert.assertEquals(property.getName(), "arrayOfInt");
        Assert.assertEquals(property.getMappedName(), "arrayOfInt");
    }

    @Test
    public void basicFieldMapping() {
        final PropertyModel property = getMappedField("name");

        Assert.assertTrue(property.isScalarValue());
        Assert.assertSame(property.getType(), String.class);
        Assert.assertEquals(property.getName(), "name");
        Assert.assertEquals(property.getMappedName(), "n");
    }

    @Test
    public void collectionFieldMapping() {
        final PropertyModel property = getMappedField("listOfString");

        Assert.assertFalse(property.isScalarValue());
        Assert.assertTrue(property.isMultipleValues());
        Assert.assertFalse(property.isArray());
        Assert.assertSame(property.getType(), List.class);
        Assert.assertSame(property.getNormalizedType(), String.class);
        Assert.assertEquals(property.getName(), "listOfString");
        Assert.assertEquals(property.getMappedName(), "listOfString");
    }

    @Test
    public void idFieldMapping() {
        final PropertyModel property = getMappedField("id");

        Assert.assertTrue(property.isScalarValue());
        Assert.assertSame(property.getType(), ObjectId.class);
        Assert.assertEquals(property.getName(), "id");
        Assert.assertEquals(property.getMappedName(), "_id");
    }

    @BeforeMethod
    public void mapping() {
        getMapper().map(List.of(TestEntity.class));
        entityModel = getMapper().getEntityModel(TestEntity.class);
    }

    @Test
    public void nestedCollectionsMapping() {
        final PropertyModel property = getMappedField("listOfListOfString");

        Assert.assertFalse(property.isScalarValue());
        Assert.assertTrue(property.isMultipleValues());
        Assert.assertFalse(property.isArray());
        Assert.assertSame(property.getType(), List.class);

        final TypeData<?> typeData = property.getTypeData();
        Assert.assertSame(typeData.getType(), List.class);

        Assert.assertEquals(typeData.getTypeParameters().get(0).getTypeParameters().get(0).getType(), String.class);
        Assert.assertEquals(property.getName(), "listOfListOfString");
        Assert.assertEquals(property.getMappedName(), "listOfListOfString");

        final List<List<String>> list = new ArrayList<>();
        list.add(dbList("a", "b", "c"));
        list.add(dbList("d", "e", "f"));
        TestEntity testEntity = new TestEntity();
        testEntity.listOfListOfString = list;
        getDs().save(testEntity);

        Assert.assertEquals(list, getDs().find(TestEntity.class)
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
