package dev.morphia.mapping;


import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MapperOptions.Builder;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
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

import static dev.morphia.query.experimental.filters.Filters.ne;


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
    public void customDiscriminators() {
        getDs().getMapper().map(EntityDiscriminator.class, EntityDiscriminator2.class);

        EntityDiscriminator entityDiscriminator = new EntityDiscriminator();
        entityDiscriminator.name = "entityDiscriminator";

        EntityDiscriminator2 entityDiscriminator2 = new EntityDiscriminator2();
        entityDiscriminator2.name = "entityDiscriminator2";

        getDs().save(List.of(entityDiscriminator, entityDiscriminator2));

        Query<EntityDiscriminator2> query = getDs().find(EntityDiscriminator2.class)
                                                   .filter(ne("name", "hi"));
        FindOptions options = new FindOptions()
                                  .logQuery();
        List<EntityDiscriminator2> list = query.iterator(options)
                                               .toList();
        Assert.assertEquals(getDs().getLoggedQuery(options), 1, list.size());
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

    @Test
    public void discriminator() {
        Datastore datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName(),
            MapperOptions.builder()
                         .discriminator(DiscriminatorFunction.lowerSimpleName())
                         .build());
        datastore.getMapper().map(EntityDiscriminator.class, EmbeddedDiscriminator.class, HasMap.class);

        EntityModel entityModel = datastore.getMapper().getEntityModel(EntityDiscriminator.class);
        Assert.assertEquals("_t", entityModel.getDiscriminatorKey());
        Assert.assertEquals("h", entityModel.getDiscriminator());

        entityModel = datastore.getMapper().getEntityModel(EmbeddedDiscriminator.class);
        Assert.assertEquals("_e", entityModel.getDiscriminatorKey());
        Assert.assertEquals("b", entityModel.getDiscriminator());

        entityModel = datastore.getMapper().getEntityModel(HasMap.class);
        Assert.assertEquals("_t", entityModel.getDiscriminatorKey());
        Assert.assertEquals(HasMap.class.getSimpleName().toLowerCase(), entityModel.getDiscriminator());
    }

    @Test
    public void lowercaseDefaultCollection() {
        DummyEntity entity = new DummyEntity();

        String collectionName = getMapper().getEntityModel(entity.getClass()).getCollectionName();
        Assert.assertEquals("uppercase", "dummyEntity", collectionName);

        Builder builder = MapperOptions.builder(getMapper().getOptions());
        final Datastore datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName(),
            builder.collectionNaming(NamingStrategy.lowerCase()).build());

        collectionName = datastore.getMapper().getEntityModel(entity.getClass()).getCollectionName();
        Assert.assertEquals("lowercase", "dummyentity", collectionName);
    }

    private void shouldFindField(Datastore datastore, HasList hl, List<String> expected) {
        datastore.save(hl);
        final Document document = getDocumentCollection(HasList.class).find().first();
        Assert.assertTrue("Should find the field", document.containsKey("names"));
        Assert.assertEquals(expected, datastore.find(HasList.class).iterator(new FindOptions().limit(1))
                                               .tryNext()
                                          .names);
        cleanup();
    }

    private Datastore empties(boolean storeEmpties) {
        Builder builder = MapperOptions.builder(getMapper().getOptions());
        return Morphia.createDatastore(getMongoClient(), getDatabase().getName(),
            builder.storeEmpties(storeEmpties).build());
    }

    private void shouldFindField(Datastore datastore, HasMap hl, Map<String, String> expected) {
        final Document document;
        datastore.save(hl);
        document = getDocumentCollection(HasMap.class).find().first();
        Assert.assertTrue("Should find the field", document.containsKey("properties"));
        Assert.assertEquals(expected, datastore.find(HasMap.class).iterator(new FindOptions().limit(1))
                                               .tryNext()
                                          .properties);
        cleanup();
    }

    private void shouldFindField(Datastore datastore,
                                 HasCollectionValuedMap hm,
                                 Map<String, Collection<String>> expected) {
        final Document document;
        datastore.save(hm);
        document = getDocumentCollection(HasCollectionValuedMap.class).find().first();
        Assert.assertTrue("Should find the field", document.containsKey("properties"));
        Assert.assertEquals(expected, datastore.find(HasCollectionValuedMap.class).iterator(new FindOptions().limit(1))
                                               .tryNext()
                                          .properties);
        cleanup();
    }

    private void shouldFindField(Datastore datastore, HasComplexObjectValuedMap hm, Map<String, ComplexObject> expected) {
        final Document document;
        datastore.save(hm);
        document = getDocumentCollection(HasComplexObjectValuedMap.class).find().first();
        Assert.assertTrue("Should find the field", document.containsKey("properties"));
        Assert.assertEquals(expected, datastore.find(HasComplexObjectValuedMap.class).iterator(new FindOptions().limit(1))
                                               .tryNext()
                                          .properties);
        cleanup();
    }

    private void shouldNotFindField(Datastore datastore, HasCollectionValuedMap hm) {
        datastore.save(hm);
        Document document = getDocumentCollection(HasCollectionValuedMap.class).find().first();
        Assert.assertFalse("field should not exist, value = " + document.get("properties"), document.containsKey("properties"));
        Assert.assertNull(datastore.find(HasCollectionValuedMap.class).iterator(new FindOptions().limit(1))
                                   .tryNext()
                              .properties);
        cleanup();
    }

    private void shouldNotFindField(Datastore datastore, HasMap hl) {
        datastore.save(hl);
        Document document = getDocumentCollection(HasMap.class).find().first();
        Assert.assertFalse("field should not exist, value = " + document.get("properties"), document.containsKey("properties"));
        Assert.assertNull(datastore.find(HasMap.class).iterator(new FindOptions().limit(1))
                                   .tryNext()
                              .properties);
        cleanup();
    }

    private void shouldNotFindField(Datastore datastore, HasComplexObjectValuedMap hm) {
        datastore.save(hm);
        Document document = getDocumentCollection(HasComplexObjectValuedMap.class).find().first();
        Assert.assertFalse("field should not exist, value = " + document.get("properties"), document.containsKey("properties"));
        Assert.assertNull(datastore.find(HasComplexObjectValuedMap.class).iterator(new FindOptions().limit(1))
                                   .tryNext()
                              .properties);
        cleanup();
    }

    private void shouldNotFindField(Datastore datastore, HasList hl) {
        datastore.save(hl);
        Document document = getDocumentCollection(HasList.class).find().first();
        Assert.assertFalse("field should not exist, value = " + document.get("names"), document.containsKey("names"));
        HasList hasList = datastore.find(HasList.class).iterator(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(hasList.names);
        cleanup();
    }

    private Datastore nulls(boolean storeNulls) {
        Builder builder = MapperOptions.builder(getMapper().getOptions());
        return Morphia.createDatastore(getMongoClient(), getDatabase().getName(),
            builder.storeNulls(storeNulls).build());
    }

    @Entity
    private static class HasList implements Serializable {
        @Id
        private final ObjectId id = new ObjectId();
        private List<String> names;

        HasList() {
        }
    }

    @Entity
    private static class HasMap implements Serializable {
        @Id
        private final ObjectId id = new ObjectId();
        private Map<String, String> properties;

        HasMap() {
        }
    }

    @Entity
    private static class HasCollectionValuedMap implements Serializable {
        @Id
        private final ObjectId id = new ObjectId();
        private Map<String, Collection<String>> properties;

        HasCollectionValuedMap() {
        }
    }

    @Entity
    private static class HasComplexObjectValuedMap implements Serializable {
        @Id
        private final ObjectId id = new ObjectId();
        private Map<String, ComplexObject> properties;

        HasComplexObjectValuedMap() {
        }
    }

    @Entity
    private static class DummyEntity {
        @Id
        private ObjectId id;
    }

    @Entity
    private static class ComplexObject {
        private String stringVal;
        private int intVal;
    }

    @Entity(value = "discriminator", discriminatorKey = "_t", discriminator = "h")
    private static class EntityDiscriminator {
        @Id
        private ObjectId id;
        private String name;
    }

    @Entity(value = "discriminator", discriminatorKey = "_t", discriminator = "h2")
    private static class EntityDiscriminator2 {
        @Id
        private ObjectId id;
        private String name;
    }

    @Entity(discriminatorKey = "_e", discriminator = "b")
    private static class EmbeddedDiscriminator {
    }
}
