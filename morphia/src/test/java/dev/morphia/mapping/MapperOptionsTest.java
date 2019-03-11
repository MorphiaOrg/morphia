package dev.morphia.mapping;


import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;


/**
 * @author scott hernandez
 * @author RainoBoy97
 */
public class MapperOptionsTest extends TestBase {

    @Test
    public void emptyListStoredWithOptions() throws Exception {
        final HasList hl = new HasList();
        hl.names = new ArrayList<String>();

        //Test default behavior
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        shouldNotFindField(hl);

        //Test default storing empty list/array with storeEmpties option
        getMorphia().getMapper().getOptions().setStoreEmpties(true);
        shouldFindField(hl, new ArrayList<String>());

        //Test opposite from above
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        shouldNotFindField(hl);

        hl.names = null;
        //Test default behavior
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        shouldNotFindField(hl);

        //Test default storing empty list/array with storeEmpties option
        getMorphia().getMapper().getOptions().setStoreEmpties(true);
        shouldNotFindField(hl);

        //Test opposite from above
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        shouldNotFindField(hl);
    }

    @Test
    public void emptyMapStoredWithOptions() throws Exception {
        final HasMap hm = new HasMap();
        hm.properties = new HashMap<String, String>();

        //Test default behavior
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        shouldNotFindField(hm);

        //Test default storing empty map with storeEmpties option
        getMorphia().getMapper().getOptions().setStoreEmpties(true);
        shouldFindField(hm, new HashMap<String, String>());


        //Test opposite from above
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        shouldNotFindField(hm);
    }

    @Test
    public void emptyCollectionValuedMapStoredWithOptions() throws Exception {
        final HasCollectionValuedMap hm = new HasCollectionValuedMap();
        hm.properties = new HashMap<String, Collection<String>>();

        //Test default behavior
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        shouldNotFindField(hm);

        //Test default storing empty map with storeEmpties option
        getMorphia().getMapper().getOptions().setStoreEmpties(true);
        shouldFindField(hm, new HashMap<String, Collection<String>>());

        //Test opposite from above
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        shouldNotFindField(hm);
    }

    @Test
    public void emptyComplexObjectValuedMapStoredWithOptions() throws Exception {
        final HasComplexObjectValuedMap hm = new HasComplexObjectValuedMap();
        hm.properties = new HashMap<String, ComplexObject>();

        //Test default behavior
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        shouldNotFindField(hm);

        //Test default storing empty map with storeEmpties option
        getMorphia().getMapper().getOptions().setStoreEmpties(true);
        shouldFindField(hm, new HashMap<String, ComplexObject>());

        //Test opposite from above
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        shouldNotFindField(hm);
    }

    @Test
    public void lowercaseDefaultCollection() {
        DummyEntity entity = new DummyEntity();

        String collectionName = getMorphia().getMapper().getCollectionName(entity);
        Assert.assertEquals("uppercase", "DummyEntity", collectionName);

        getMorphia().getMapper().getOptions().setUseLowerCaseCollectionNames(true);

        collectionName = getMorphia().getMapper().getCollectionName(entity);
        Assert.assertEquals("lowercase", "dummyentity", collectionName);
    }

    @Test
    public void nullListStoredWithOptions() throws Exception {
        final HasList hl = new HasList();
        hl.names = null;

        //Test default behavior
        getMorphia().getMapper().getOptions().setStoreNulls(false);
        shouldNotFindField(hl);

        //Test default storing null list/array with storeNulls option
        getMorphia().getMapper().getOptions().setStoreNulls(true);
        shouldFindField(hl, null);

        //Test opposite from above
        getMorphia().getMapper().getOptions().setStoreNulls(false);
        shouldNotFindField(hl);
    }

    @Test
    public void nullMapStoredWithOptions() throws Exception {
        final HasMap hm = new HasMap();
        hm.properties = null;

        //Test default behavior
        getMorphia().getMapper().getOptions().setStoreNulls(false);
        shouldNotFindField(hm);

        //Test default storing empty map with storeEmpties option
        getMorphia().getMapper().getOptions().setStoreNulls(true);
        shouldFindField(hm, null);


        //Test opposite from above
        getMorphia().getMapper().getOptions().setStoreNulls(false);
        shouldNotFindField(hm);
    }

    private void shouldFindField(final HasList hl, final List<String> expected) {
        getDs().save(hl);
        final DBObject dbObj = getDs().getCollection(HasList.class).findOne();
        Assert.assertTrue("Should find the field", dbObj.containsField("names"));
        Assert.assertEquals(expected, getDs().find(HasList.class)
                                             .find(new FindOptions().limit(1))
                                             .tryNext()
                                          .names);
    }

    private void shouldFindField(final HasMap hl, final Map<String, String> expected) {
        final DBObject dbObj;
        getDs().save(hl);
        dbObj = getDs().getCollection(HasMap.class).findOne();
        Assert.assertTrue("Should find the field", dbObj.containsField("properties"));
        Assert.assertEquals(expected, getDs().find(HasMap.class)
                                             .find(new FindOptions().limit(1))
                                             .tryNext()
                                          .properties);
    }

    private void shouldFindField(final HasCollectionValuedMap hm, final Map<String, Collection<String>> expected) {
        final DBObject dbObj;
        getDs().save(hm);
        dbObj = getDs().getCollection(HasCollectionValuedMap.class).findOne();
        Assert.assertTrue("Should find the field", dbObj.containsField("properties"));
        Assert.assertEquals(expected, getDs().find(HasCollectionValuedMap.class)
                                             .find(new FindOptions().limit(1))
                                             .tryNext()
                                          .properties);
    }

    private void shouldFindField(final HasComplexObjectValuedMap hm, final Map<String, ComplexObject> expected) {
        final DBObject dbObj;
        getDs().save(hm);
        dbObj = getDs().getCollection(HasComplexObjectValuedMap.class).findOne();
        Assert.assertTrue("Should find the field", dbObj.containsField("properties"));
        Assert.assertEquals(expected, getDs().find(HasComplexObjectValuedMap.class)
                                             .find(new FindOptions().limit(1))
                                             .tryNext()
                                          .properties);
    }

    private void shouldNotFindField(final HasMap hl) {
        getDs().save(hl);
        DBObject dbObj = getDs().getCollection(HasMap.class).findOne();
        Assert.assertFalse("field should not exist, value = " + dbObj.get("properties"), dbObj.containsField("properties"));
        Assert.assertNull(getDs().find(HasMap.class)
                                 .find(new FindOptions().limit(1))
                                 .tryNext()
                              .properties);
    }

    private void shouldNotFindField(final HasList hl) {
        getDs().save(hl);
        DBObject dbObj = getDs().getCollection(HasList.class).findOne();
        Assert.assertFalse("field should not exist, value = " + dbObj.get("names"), dbObj.containsField("names"));
        Assert.assertNull(getDs().find(HasList.class)
                                 .find(new FindOptions().limit(1))
                                 .tryNext()
                              .names);
    }

    private void shouldNotFindField(final HasCollectionValuedMap hm) {
        getDs().save(hm);
        DBObject dbObj = getDs().getCollection(HasCollectionValuedMap.class).findOne();
        Assert.assertFalse("field should not exist, value = " + dbObj.get("properties"), dbObj.containsField("properties"));
        Assert.assertNull(getDs().find(HasCollectionValuedMap.class)
                                 .find(new FindOptions().limit(1))
                                 .tryNext()
                              .properties);
    }

    private void shouldNotFindField(final HasComplexObjectValuedMap hm) {
        getDs().save(hm);
        DBObject dbObj = getDs().getCollection(HasComplexObjectValuedMap.class).findOne();
        Assert.assertFalse("field should not exist, value = " + dbObj.get("properties"), dbObj.containsField("properties"));
        Assert.assertNull(getDs().find(HasComplexObjectValuedMap.class)
                                 .find(new FindOptions().limit(1))
                                 .tryNext()
                              .properties);
    }

    private static class HasList implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        private List<String> names;

        HasList() {
        }
    }

    private static class HasMap implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        private Map<String, String> properties;

        HasMap() {
        }
    }

    private static class HasCollectionValuedMap implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        private Map<String, Collection<String>> properties;

        HasCollectionValuedMap() {
        }
    }

    private static class HasComplexObjectValuedMap implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        private Map<String, ComplexObject> properties;

        HasComplexObjectValuedMap() {
        }
    }

    @Entity
    private static class DummyEntity {
    }

    private static class ComplexObject {
        private String stringVal;
        private int intVal;
    }
}
