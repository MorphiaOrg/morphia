package dev.morphia.mapping;


import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MapperOptions.Builder;
import dev.morphia.query.FindOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MapperOptionsTest extends TestBase {

    @Test
    public void emptyListStoredWithOptions() {
        final HasList hl = new HasList();
        hl.names = new ArrayList<>();

        //Test default behavior
        shouldNotFindField(empties(false), hl);

        //Test default storing empty list/array with storeEmpties option
        shouldFindField(empties(true), hl, new ArrayList<>());

        //Test opposite from above
        shouldNotFindField(empties(false), hl);

        hl.names = null;
        //Test default behavior
        shouldNotFindField(empties(false), hl);

        //Test default storing empty list/array with storeEmpties option
        shouldNotFindField(empties(true), hl);

        //Test opposite from above
        shouldNotFindField(empties(false), hl);
    }

    @Test
    public void emptyMapStoredWithOptions() {
        final HasMap hm = new HasMap();
        hm.properties = new HashMap<>();

        //Test default behavior
        shouldNotFindField(empties(false), hm);

        //Test default storing empty map with storeEmpties option
        shouldFindField(empties(true), hm, new HashMap<>());

        //Test opposite from above
        shouldNotFindField(empties(false), hm);
    }

    private Datastore empties(final boolean storeEmpties) {
        Builder builder = MapperOptions.builder(getMapper().getOptions());
        return Morphia.createDatastore(getMongoClient(), getDatabase().getName(),
            builder.storeEmpties(storeEmpties).build());
    }

    private Datastore nulls(final boolean storeNulls) {
        Builder builder = MapperOptions.builder(getMapper().getOptions());
        return Morphia.createDatastore(getMongoClient(), getDatabase().getName(),
            builder.storeNulls(storeNulls).build());
    }

    @Test
    public void emptyCollectionValuedMapStoredWithOptions() {
        final HasCollectionValuedMap hm = new HasCollectionValuedMap();
        hm.properties = new HashMap<>();

        //Test default behavior
        shouldNotFindField(empties(false), hm);

        //Test default storing empty map with storeEmpties option
        shouldFindField(empties(true), hm, new HashMap<>());

        //Test opposite from above
        shouldNotFindField(empties(false), hm);
    }

    @Test
    public void emptyComplexObjectValuedMapStoredWithOptions() {
        final HasComplexObjectValuedMap hm = new HasComplexObjectValuedMap();
        hm.properties = new HashMap<>();

        //Test default behavior
        shouldNotFindField(empties(false), hm);

        //Test default storing empty map with storeEmpties option
        shouldFindField(empties(true), hm, new HashMap<>());

        //Test opposite from above
        shouldNotFindField(empties(false), hm);
    }

    @Test
    public void lowercaseDefaultCollection() {
        DummyEntity entity = new DummyEntity();

        String collectionName = getMapper().getCollectionName(entity);
        Assert.assertEquals("uppercase", "DummyEntity", collectionName);

        Builder builder = MapperOptions.builder(getMapper().getOptions());
        final Datastore datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName(),
            builder.useLowerCaseCollectionNames(true).build());

        collectionName = datastore.getMapper().getCollectionName(entity);
        Assert.assertEquals("lowercase", "dummyentity", collectionName);
    }

    @Test
    public void nullListStoredWithOptions() {
        final HasList hl = new HasList();
        hl.names = null;

        //Test default behavior
        shouldNotFindField(nulls(false), hl);

        //Test default storing null list/array with storeNulls option
        shouldFindField(nulls(true), hl, null);

        //Test opposite from above
        shouldNotFindField(nulls(false), hl);
    }

    @Test
    public void nullMapStoredWithOptions() {
        final HasMap hm = new HasMap();
        hm.properties = null;

        //Test default behavior
        shouldNotFindField(nulls(false), hm);

        //Test default storing empty map with storeEmpties option
        shouldFindField(nulls(true), hm, null);


        //Test opposite from above
        shouldNotFindField(nulls(false), hm);
    }

    private void shouldFindField(final Datastore datastore, final HasList hl, final List<String> expected) {
        datastore.save(hl);
        final Document document = datastore.getCollection(HasList.class).find().first();
        Assert.assertTrue("Should find the field", document.containsKey("names"));
        Assert.assertEquals(expected, datastore.find(HasList.class)
                                             .execute(new FindOptions().limit(1))
                                             .tryNext()
                                          .names);
    }

    private void shouldFindField(final Datastore datastore, final HasMap hl, final Map<String, String> expected) {
        final Document document;
        datastore.save(hl);
        document = datastore.getCollection(HasMap.class).find().first();
        Assert.assertTrue("Should find the field", document.containsKey("properties"));
        Assert.assertEquals(expected, datastore.find(HasMap.class)
                                             .execute(new FindOptions().limit(1))
                                             .tryNext()
                                          .properties);
    }

    private void shouldFindField(final Datastore datastore, final HasCollectionValuedMap hm, final Map<String, Collection<String>> expected) {
        final Document document;
        datastore.save(hm);
        document = datastore.getCollection(HasCollectionValuedMap.class).find().first();
        Assert.assertTrue("Should find the field", document.containsKey("properties"));
        Assert.assertEquals(expected, datastore.find(HasCollectionValuedMap.class)
                                             .execute(new FindOptions().limit(1))
                                             .tryNext()
                                          .properties);
    }

    private void shouldFindField(final Datastore datastore, final HasComplexObjectValuedMap hm, final Map<String, ComplexObject> expected) {
        final Document document;
        datastore.save(hm);
        document = datastore.getCollection(HasComplexObjectValuedMap.class).find().first();
        Assert.assertTrue("Should find the field", document.containsKey("properties"));
        Assert.assertEquals(expected, datastore.find(HasComplexObjectValuedMap.class)
                                             .execute(new FindOptions().limit(1))
                                             .tryNext()
                                          .properties);
    }

    private void shouldNotFindField(final Datastore datastore, final HasMap hl) {
        datastore.save(hl);
        Document document = datastore.getCollection(HasMap.class).find().first();
        Assert.assertFalse("field should not exist, value = " + document.get("properties"), document.containsKey("properties"));
        Assert.assertNull(datastore.find(HasMap.class)
                                 .execute(new FindOptions().limit(1))
                                 .tryNext()
                              .properties);
    }

    private void shouldNotFindField(final Datastore datastore, final HasList hl) {
        datastore.save(hl);
        Document document = datastore.getCollection(HasList.class).find().first();
        Assert.assertFalse("field should not exist, value = " + document.get("names"), document.containsKey("names"));
        Assert.assertNull(datastore.find(HasList.class)
                                 .execute(new FindOptions().limit(1))
                                 .tryNext()
                              .names);
    }

    private void shouldNotFindField(final Datastore datastore, final HasCollectionValuedMap hm) {
        datastore.save(hm);
        Document document = datastore.getCollection(HasCollectionValuedMap.class).find().first();
        Assert.assertFalse("field should not exist, value = " + document.get("properties"), document.containsKey("properties"));
        Assert.assertNull(datastore.find(HasCollectionValuedMap.class)
                                 .execute(new FindOptions().limit(1))
                                 .tryNext()
                              .properties);
    }

    private void shouldNotFindField(final Datastore datastore, final HasComplexObjectValuedMap hm) {
        datastore.save(hm);
        Document document = datastore.getCollection(HasComplexObjectValuedMap.class).find().first();
        Assert.assertFalse("field should not exist, value = " + document.get("properties"), document.containsKey("properties"));
        Assert.assertNull(datastore.find(HasComplexObjectValuedMap.class)
                                 .execute(new FindOptions().limit(1))
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
