package dev.morphia.test.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import static dev.morphia.Morphia.*;
import static dev.morphia.mapping.NamingStrategy.lowerCase;
import static dev.morphia.query.filters.Filters.ne;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@SuppressWarnings({ "ConstantConditions", "unused" })
public class TestConfigOptions extends TestBase {

    @Test
    public void emptyListStoredWithOptions() {
        final HasList hl = new HasList();
        hl.names = new ArrayList<>();

        //Test default behavior
        empties(false, () -> {
            shouldNotFindField(getDs(), hl);
        });

        //Test default storing empty list/array with storeEmpties option
        empties(true, () -> {
            shouldFindField(getDs(), hl, new ArrayList<>());
        });

        //Test opposite from above
        empties(false, () -> {
            shouldNotFindField(getDs(), hl);
        });

        hl.names = null;
        //Test default behavior
        empties(false, () -> {
            shouldNotFindField(getDs(), hl);
        });

        //Test default storing empty list/array with storeEmpties option
        empties(true, () -> {
            shouldNotFindField(getDs(), hl);
        });

        //Test opposite from above
        empties(false, () -> {
            shouldNotFindField(getDs(), hl);
        });
    }

    @Test
    public void emptyMapStoredWithOptions() {
        final HasMap hm = new HasMap();
        hm.properties = new HashMap<>();

        //Test default behavior
        empties(false, () -> {
            shouldNotFindField(getDs(), hm);
        });

        //Test default storing empty map with storeEmpties option
        empties(true, () -> {
            shouldFindField(getDs(), hm, new HashMap<>());
        });

        //Test opposite from above
        empties(false, () -> {
            shouldNotFindField(getDs(), hm);
        });
    }

    @Test
    public void emptyCollectionValuedMapStoredWithOptions() {
        final HasCollectionValuedMap hm = new HasCollectionValuedMap();
        hm.properties = new HashMap<>();

        //Test default behavior
        empties(false, () -> {
            shouldNotFindField(getDs(), hm);
        });

        //Test default storing empty map with storeEmpties option
        empties(true, () -> {
            shouldFindField(getDs(), hm, new HashMap<>());
        });

        //Test opposite from above
        empties(false, () -> {
            shouldNotFindField(getDs(), hm);
        });
    }

    @Test
    public void emptyComplexObjectValuedMapStoredWithOptions() {
        final HasComplexObjectValuedMap hm = new HasComplexObjectValuedMap();
        hm.properties = new HashMap<>();

        //Test default behavior
        empties(false, () -> {
            shouldNotFindField(getDs(), hm);
        });

        //Test default storing empty map with storeEmpties option
        empties(true, () -> {
            shouldFindField(getDs(), hm, new HashMap<>());
        });

        //Test opposite from above
        empties(false, () -> {
            shouldNotFindField(getDs(), hm);
        });
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
        List<EntityDiscriminator2> list = query.iterator(new FindOptions().logQuery()).toList();
        assertEquals(list.size(), 1, query.getLoggedQuery());
    }

    @Test
    public void nullListStoredWithOptions() {
        final HasList hl = new HasList();
        hl.names = null;

        //Test default behavior
        nulls(false, () -> {
            shouldNotFindField(getDs(), hl);
        });

        //Test default storing null list/array with storeNulls option
        nulls(true, () -> {
            shouldFindField(getDs(), hl, null);
        });

        //Test opposite from above
        nulls(false, () -> {
            shouldNotFindField(getDs(), hl);
        });
    }

    @Test
    public void nullMapStoredWithOptions() {
        final HasMap hm = new HasMap();
        hm.properties = null;

        //Test default behavior
        nulls(false, () -> {
            shouldNotFindField(getDs(), hm);
        });

        //Test default storing empty map with storeEmpties option
        nulls(true, () -> {
            shouldFindField(getDs(), hm, null);
        });

        //Test opposite from above
        nulls(false, () -> {
            shouldNotFindField(getDs(), hm);
        });
    }

    @Test
    public void discriminator() {
        Datastore datastore = createDatastore(getMongoClient(), getDatabase().getName(),
                MapperOptions.builder()
                        .discriminator(DiscriminatorFunction.lowerSimpleName())
                        .build());
        datastore.getMapper().map(EntityDiscriminator.class, EmbeddedDiscriminator.class, HasMap.class);

        EntityModel entityModel = datastore.getMapper().getEntityModel(EntityDiscriminator.class);
        assertEquals(entityModel.getDiscriminatorKey(), "_t");
        assertEquals(entityModel.getDiscriminator(), "h");

        entityModel = datastore.getMapper().getEntityModel(EmbeddedDiscriminator.class);
        assertEquals(entityModel.getDiscriminatorKey(), "_e");
        assertEquals(entityModel.getDiscriminator(), "b");

        entityModel = datastore.getMapper().getEntityModel(HasMap.class);
        assertEquals(entityModel.getDiscriminatorKey(), "_t");
        assertEquals(entityModel.getDiscriminator(), HasMap.class.getSimpleName().toLowerCase());
    }

    @Test
    public void lowercaseDefaultCollection() {
        DummyEntity entity = new DummyEntity();

        String collectionName = getMapper().getEntityModel(entity.getClass()).getCollectionName();
        assertEquals(collectionName, "dummyEntity", "camelCase");

        withConfig(buildConfig()
                .collectionNaming(lowerCase()), () -> {

                    String name = getMapper().getEntityModel(entity.getClass()).getCollectionName();
                    assertEquals(name, "dummyentity", "lowercase");
                });
    }

    private void shouldFindField(Datastore datastore, HasList hl, List<String> expected) {
        datastore.save(hl);
        final Document document = getDocumentCollection(HasList.class).find().first();
        assertTrue(document.containsKey("names"), "Should find the field");
        assertEquals(datastore.find(HasList.class).first().names, expected);
        cleanup();
    }

    private void shouldFindField(Datastore datastore, HasMap hl, Map<String, String> expected) {
        final Document document;
        datastore.save(hl);
        document = getDocumentCollection(HasMap.class).find().first();
        assertTrue(document.containsKey("properties"), "Should find the field");
        assertEquals(datastore.find(HasMap.class).iterator(new FindOptions().limit(1))
                .tryNext().properties, expected);
        cleanup();
    }

    private void shouldFindField(Datastore datastore,
            HasCollectionValuedMap hm,
            Map<String, Collection<String>> expected) {
        final Document document;
        datastore.save(hm);
        document = getDocumentCollection(HasCollectionValuedMap.class).find().first();
        assertTrue(document.containsKey("properties"), "Should find the field");
        assertEquals(datastore.find(HasCollectionValuedMap.class).first().properties, expected);
        cleanup();
    }

    private void shouldFindField(Datastore datastore, HasComplexObjectValuedMap hm, Map<String, ComplexObject> expected) {
        final Document document;
        datastore.save(hm);
        document = getDocumentCollection(HasComplexObjectValuedMap.class).find().first();
        assertTrue(document.containsKey("properties"), "Should find the field");
        assertEquals(datastore.find(HasComplexObjectValuedMap.class).first().properties, expected);
        cleanup();
    }

    private void shouldNotFindField(Datastore datastore, HasCollectionValuedMap hm) {
        datastore.save(hm);
        Document document = getDocumentCollection(HasCollectionValuedMap.class).find().first();
        assertFalse(document.containsKey("properties"), "field should not exist, value = " + document.get("properties"));
        assertNull(datastore.find(HasCollectionValuedMap.class).first().properties);
        cleanup();
    }

    private void shouldNotFindField(Datastore datastore, HasMap hl) {
        datastore.save(hl);
        Document document = getDocumentCollection(HasMap.class).find().first();
        assertFalse(document.containsKey("properties"), "field should not exist, value = " + document.get("properties"));
        assertNull(datastore.find(HasMap.class).first().properties);
        cleanup();
    }

    private void shouldNotFindField(Datastore datastore, HasComplexObjectValuedMap hm) {
        datastore.save(hm);
        Document document = getDocumentCollection(HasComplexObjectValuedMap.class).find().first();
        assertFalse(document.containsKey("properties"), "field should not exist, value = " + document.get("properties"));
        assertNull(datastore.find(HasComplexObjectValuedMap.class).first().properties);
        cleanup();
    }

    private void shouldNotFindField(Datastore datastore, HasList hl) {
        datastore.save(hl);
        Document document = getDocumentCollection(HasList.class).find().first();
        assertFalse(document.containsKey("names"), "field should not exist, value = " + document.get("names"));
        HasList hasList = datastore.find(HasList.class).iterator(new FindOptions().limit(1))
                .tryNext();
        assertNull(hasList.names);
        cleanup();
    }

    private void empties(boolean storeEmpties, Runnable body) {
        withConfig(buildConfig().storeEmpties(storeEmpties), body);
    }

    private void nulls(boolean storeNulls, Runnable body) {
        withConfig(buildConfig().storeNulls(storeNulls), body);
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
