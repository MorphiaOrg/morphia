package org.mongodb.morphia.mapping;


import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.dao.BasicDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class CollectionOfValuesTest extends TestBase {

    @Test
    public void testCity() {
        getMorphia().map(City.class);

        City city = new City();
        city.name = "My city";
        city.array = new byte[]{4, 5};
        for (byte i = 0; i < 2; i++) {
            for (byte j = 0; j < 2; j++) {
                city.cells[i][j] = (i * 100 + j);
            }
        }

        getDs().save(city);
        City loaded = getDs().get(city);
        Assert.assertEquals(city.name, loaded.name);
        compare(city.array, loaded.array);
        for (int i = 0; i < city.cells.length; i++) {
            compare(city.cells[i], loaded.cells[i]);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateEntityWithBasicDBList() throws Exception {
        BasicDAO<TestEntity, ObjectId> dao;
        dao = new BasicDAO<TestEntity, ObjectId>(TestEntity.class, getDs());

        TestEntity entity = new TestEntity();

        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        data.add(new BasicDBObject("type", "text")
                     .append("data", new BasicDBObject("text", "sometext")));

        entity.setData(data);
        dao.save(entity);

        final TestEntity fetched = dao.get(entity.getId());
        Assert.assertEquals(entity, fetched);
    }

    @Test
    public void testListOfListMapping() throws Exception {
        getMorphia().map(ContainsListOfList.class);
        getDs().delete(getDs().find(ContainsListOfList.class));
        final ContainsListOfList entity = new ContainsListOfList();
        final List<List<String>> testList = new ArrayList<List<String>>();
        final List<String> element1 = new ArrayList<String>();
        element1.add("element1");
        testList.add(element1);

        final List<String> element2 = new ArrayList<String>();
        element2.add("element2");
        testList.add(element2);

        entity.listOfList = testList;
        getDs().save(entity);
        final ContainsListOfList loaded = getDs().get(entity);

        Assert.assertNotNull(loaded.listOfList);

        Assert.assertEquals(testList, loaded.listOfList);
        final List<String> loadedElement1 = loaded.listOfList.get(0);
        Assert.assertEquals(element1, loadedElement1);
        Assert.assertEquals(element1.get(0), loadedElement1.get(0));
        Assert.assertNotNull(loaded.id);
    }

    @Test
    public void testTwoDimensionalArrayMapping() throws Exception {
        getMorphia().map(ContainsTwoDimensionalArray.class);
        final ContainsTwoDimensionalArray entity = new ContainsTwoDimensionalArray();
        entity.oneDimArray = "Joseph".getBytes();
        entity.twoDimArray = new byte[][]{"Joseph".getBytes(), "uwe".getBytes()};
        getDs().save(entity);
        final ContainsTwoDimensionalArray loaded = getDs().get(ContainsTwoDimensionalArray.class, entity.id);
        Assert.assertNotNull(loaded.id);
        Assert.assertNotNull(loaded.oneDimArray);
        Assert.assertNotNull(loaded.twoDimArray);

        compare(entity.oneDimArray, loaded.oneDimArray);

        compare(entity.twoDimArray[0], loaded.twoDimArray[0]);
        compare(entity.twoDimArray[1], loaded.twoDimArray[1]);
    }

    private void compare(final byte[] left, final byte[] right) {
        Assert.assertArrayEquals(left, right);
    }

    private void compare(final int[] left, final int[] right) {
        Assert.assertArrayEquals(left, right);
    }

    @Entity("CreateEntityWithDBListIT-TestEntity")
    public static class TestEntity {

        @Id
        private ObjectId id;
        private BasicDBList data;

        public BasicDBList getData() {
            return data;
        }

        public void setData(final List<?> data) {
            this.data = new BasicDBList();
            this.data.addAll(data);
        }

        public ObjectId getId() {
            return id;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (data != null ? data.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final TestEntity that = (TestEntity) o;

            if (id != null ? !id.equals(that.id) : that.id != null) {
                return false;
            }
            return !(data != null ? !data.equals(that.data) : that.data != null);

        }
    }

    public static class ContainsListOfList {
        @Id
        private ObjectId id;
        @Embedded
        private List<List<String>> listOfList;
    }

    public static class ContainsTwoDimensionalArray {
        @Id
        private ObjectId id;
        private byte[] oneDimArray;
        private byte[][] twoDimArray;
    }

    @Entity(noClassnameStored = true)
    public static class City {
        @Id
        private ObjectId id;
        private String name;
        @Embedded
        private byte[] array;
        private int[][] cells = new int[2][2];
    }

}
