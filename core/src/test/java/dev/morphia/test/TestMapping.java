package dev.morphia.test;

import com.mongodb.client.model.Filters;
import dev.morphia.Datastore;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.LoadOnly;
import dev.morphia.annotations.Name;
import dev.morphia.annotations.Transient;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.QueryFactory;
import dev.morphia.test.models.Author;
import dev.morphia.test.models.BannedUser;
import dev.morphia.test.models.BlogImage;
import dev.morphia.test.models.Book;
import dev.morphia.test.models.CityPopulation;
import dev.morphia.test.models.Jpg;
import dev.morphia.test.models.MappedInterface;
import dev.morphia.test.models.MappedInterfaceImpl;
import dev.morphia.test.models.Png;
import dev.morphia.test.models.State;
import dev.morphia.test.models.TestEntity;
import dev.morphia.test.models.User;
import dev.morphia.test.models.errors.ContainsDocument;
import dev.morphia.test.models.errors.ContainsMapLike;
import dev.morphia.test.models.errors.ContainsXKeyMap;
import dev.morphia.test.models.errors.OuterClass.NonStaticInnerClass;
import dev.morphia.test.models.external.HoldsUnannotated;
import dev.morphia.test.models.external.ThirdPartyEmbedded;
import dev.morphia.test.models.external.ThirdPartyEmbeddedProxy;
import dev.morphia.test.models.external.ThirdPartyEntity;
import dev.morphia.test.models.external.ThirdPartyEntityProxy;
import dev.morphia.test.models.generics.Another;
import dev.morphia.test.models.generics.Child;
import dev.morphia.test.models.generics.EmbeddedType;
import dev.morphia.test.models.methods.MethodMappedUser;
import dev.morphia.test.models.versioned.AbstractVersionedBase;
import dev.morphia.test.models.versioned.Versioned;
import dev.morphia.test.models.versioned.VersionedChildEntity;
import dev.morphia.test.models.versioned.subversioned.VersionedToo;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static dev.morphia.Morphia.createDatastore;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@SuppressWarnings({"unchecked", "unchecked"})
public class TestMapping extends TestBase {
    @Test
    public void childMapping() {
        List<EntityModel> list = getMapper().map(User.class, BannedUser.class);

        assertEquals(list.get(0).getCollectionName(), "users");
        assertEquals(list.get(1).getCollectionName(), "banned");
    }

    @Test
    public void collectionNaming() {
        MapperOptions options = MapperOptions.builder()
                                             .collectionNaming(NamingStrategy.lowerCase())
                                             .build();
        Datastore datastore = createDatastore(TestBase.TEST_DB_NAME, options);
        List<EntityModel> map = datastore.getMapper().map(ContainsMapWithEmbeddedInterface.class, ContainsIntegerList.class);

        assertEquals(map.get(0).getCollectionName(), "containsmapwithembeddedinterface");
        assertEquals(map.get(1).getCollectionName(), "cil");

        options = MapperOptions.builder()
                               .collectionNaming(NamingStrategy.kebabCase())
                               .build();
        datastore = createDatastore(TestBase.TEST_DB_NAME, options);
        map = datastore.getMapper().map(ContainsMapWithEmbeddedInterface.class, ContainsIntegerList.class);

        assertEquals(map.get(0).getCollectionName(), "contains-map-with-embedded-interface");
        assertEquals(map.get(1).getCollectionName(), "cil");
    }

    @Test
    public void constructors() {
        getDs().getMapper().map(ConstructorBased.class);

        ContainsFinalField value = new ContainsFinalField();
        ConstructorBased instance = new ConstructorBased(new ObjectId(), "test instance", MorphiaReference.wrap(value));

        getDs().save(List.of(value, instance));

        ConstructorBased first = getDs().find(ConstructorBased.class).first();
        assertNotNull(first);
        assertEquals(instance, first);
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public final void multipleIds() {
        getMapper().map(TwoIds.class);
    }

    @Test
    public void propertyNaming() {
        verify(NamingStrategy.identity(), "embeddedValues", "intList");
        verify(NamingStrategy.camelCase(), "embeddedValues", "intList");
        verify(NamingStrategy.kebabCase(), "embedded-values", "int-list");
        verify(NamingStrategy.lowerCase(), "embeddedvalues", "intlist");
        verify(NamingStrategy.snakeCase(), "embedded_values", "int_list");
    }

    @Test
    public void shouldOnlyMapEntitiesInTheGivenPackage() {
        withOptions(MapperOptions.builder(getMapper().getOptions())
                                 .build(), () -> {
            getMapper().mapPackageFromClass(Versioned.class);

            // then
            List<EntityModel> list = getMapper().getMappedEntities();
            Collection<Class<?>> classes = list.stream().map(EntityModel::getType)
                                               .collect(Collectors.toList());
            assertEquals(classes.size(), 3);
            assertTrue(classes.contains(AbstractVersionedBase.class));
            assertTrue(classes.contains(Versioned.class));
            assertTrue(classes.contains(VersionedChildEntity.class));
        });
    }

    @Test
    public void shouldSupportGenericArrays() {
        getMapper().map(MyEntity.class);
    }

    @Test
    public void subTypes() {
        getMapper().map(EmbeddedType.class, Another.class, Child.class);

        Mapper mapper = getMapper();
        List<EntityModel> subTypes = mapper.getEntityModel(EmbeddedType.class).getSubtypes();
        Assert.assertTrue(subTypes.contains(mapper.getEntityModel(Another.class)));
        Assert.assertTrue(subTypes.contains(mapper.getEntityModel(Child.class)));
    }

    @Test
    public void subtypes() {
        List<EntityModel> list = getMapper().map(MappedInterface.class, MappedInterfaceImpl.class, User.class, BannedUser.class);

        assertEquals(list.get(0).getSubtypes().size(), 1, "Should find 1 subtype: " + list.get(0));
        assertEquals(list.get(2).getSubtypes().size(), 1, "Should find 1 subtype: " + list.get(2));
    }

    @Test
    public void testAlsoLoad() {
        getMapper().map(ContainsIntegerListNew.class, ContainsIntegerList.class);
        final ContainsIntegerList cil = new ContainsIntegerList();
        cil.intList.add(1);
        getDs().save(cil);
        final ContainsIntegerList cilLoaded = getDs().find(ContainsIntegerList.class)
                                                     .filter(eq("_id", cil.id))
                                                     .first();
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(cilLoaded.intList.size(), cil.intList.size());
        assertEquals(cilLoaded.intList.get(0), cil.intList.get(0));

        final ContainsIntegerListNew cilNew = getDs().find(ContainsIntegerListNew.class).filter(eq("_id", cil.id)).first();
        assertNotNull(cilNew);
        assertNotNull(cilNew.integers);
        assertEquals(cilNew.integers.size(), 1);
        assertEquals((int) cil.intList.get(0), 1);
    }

    @Test
    public void testBadMappings() {
        assertThrows(MappingException.class, () -> {
            getMapper().map(UnannotatedEntity.class);
            fail("Missing @Entity and @Embedded should have been caught");
        });

        assertThrows(MappingException.class, () -> {
            getMapper().map(ThirdPartyEmbedded.class);
            fail("Missing @Entity and @Embedded should have been caught");
        });

        assertThrows(MappingException.class, () -> {
            getMapper().map(NonStaticInnerClass.class);
            fail("Validation: Non-static inner class allowed");
        });
    }

    @Test
    public void testBasicMapping() {
        Mapper mapper = getDs().getMapper();
        mapper.map(List.of(State.class, CityPopulation.class));

        final State state = new State();
        state.state = "NY";
        state.biggest = new CityPopulation("NYC", 8336817L);
        state.smallest = new CityPopulation("Red House", 38L);

        getDs().save(state);

        Query<State> query = getDs().find(State.class)
                                    .filter(eq("_id", state.id));
        State loaded = query.first();

        assertEquals(loaded, state);

        assertEquals(mapper.getEntityModel(State.class)
                           .getProperties().stream()
                           .map(PropertyModel::getMappedName)
                           .collect(toList()), List.of("_id", "state", "biggestCity", "smallestCity"));
    }

    @Test
    public void testByteArrayMapping() {
        getMapper().map(ContainsByteArray.class);
        final ObjectId savedKey = getDs().save(new ContainsByteArray()).id;
        final ContainsByteArray loaded = getDs().find(ContainsByteArray.class)
                                                .filter(eq("_id", savedKey))
                                                .first();
        assertEquals(new String(loaded.bytes), new String((new ContainsByteArray()).bytes));
        assertNotNull(loaded.id);
    }

    @Test
    public void testArrayOfNulls() {
        getMapper().map(ObjectArray.class);

        ObjectArray entity = new ObjectArray();
        entity.array = new ContainsCollection[10];
        entity.typedArray = new Integer[10];
        entity.byteArray = new Byte[10];
        getDs().save(entity);
    }

    private static class ObjectArray extends TestEntity {
        private ContainsCollection[] array;
        private Integer[] typedArray;
        private Byte[] byteArray;
    }

    @Test
    public void testCollectionMapping() {
        getMapper().map(ContainsCollection.class);
        final ObjectId savedKey = getDs().save(new ContainsCollection()).id;
        final ContainsCollection loaded = getDs().find(ContainsCollection.class)
                                                 .filter(eq("_id", savedKey))
                                                 .first();
        assertEquals((new ContainsCollection()).coll, loaded.coll);
        assertNotNull(loaded.id);
    }

    @Test
    public void testEmbeddedArrayElementHasNoClassname() {
        getMapper().map(ContainsEmbeddedArray.class);
        final ContainsEmbeddedArray cea = new ContainsEmbeddedArray();
        cea.res = new RenamedEmbedded[]{new RenamedEmbedded()};

        final Document document = toDocument(cea);
        List<Document> res = (List<Document>) document.get("res");
        assertFalse(res.get(0).containsKey(getMapper().getOptions().getDiscriminatorKey()));
    }

    @Test
    public void testEmbeddedDocument() {
        getMapper().map(ContainsDocument.class);
        getDs().save(new ContainsDocument());
        assertNotNull(getDs().find(ContainsDocument.class).iterator(new FindOptions().limit(1))
                             .next());
    }

    @Test
    public void testEmbeddedEntity() {
        getMapper().map(ContainsEmbeddedEntity.class);
        getDs().save(new ContainsEmbeddedEntity());
        final ContainsEmbeddedEntity ceeLoaded = getDs().find(ContainsEmbeddedEntity.class)
                                                        .iterator(new FindOptions().limit(1))
                                                        .next();
        assertNotNull(ceeLoaded);
        assertNotNull(ceeLoaded.id);
        assertNotNull(ceeLoaded.cil);
        assertNull(ceeLoaded.cil.id);

    }

    @Test
    public void testEmbeddedEntityDocumentHasNoClassname() {
        getMapper().map(ContainsEmbeddedEntity.class);
        final ContainsEmbeddedEntity cee = new ContainsEmbeddedEntity();
        cee.cil = new ContainsIntegerList();
        cee.cil.intList = Collections.singletonList(1);
        final Document document = toDocument(cee);
        assertFalse(((Document) document.get("cil")).containsKey(getMapper().getOptions().getDiscriminatorKey()));
    }

    @Test
    public void testEnumKeyedMap() {
        final ContainsEnum1KeyMap map = new ContainsEnum1KeyMap();
        map.values.put(Enum1.A, "I'm a");
        map.values.put(Enum1.B, "I'm b");
        map.embeddedValues.put(Enum1.A, "I'm a");
        map.embeddedValues.put(Enum1.B, "I'm b");

        getDs().save(map);

        final ContainsEnum1KeyMap mapLoaded = getDs().find(ContainsEnum1KeyMap.class).filter(eq("_id", map.id)).first();

        assertNotNull(mapLoaded);
        assertEquals(mapLoaded.values.size(), 2);
        assertNotNull(mapLoaded.values.get(Enum1.A));
        assertNotNull(mapLoaded.values.get(Enum1.B));
        assertEquals(mapLoaded.embeddedValues.size(), 2);
        assertNotNull(mapLoaded.embeddedValues.get(Enum1.A));
        assertNotNull(mapLoaded.embeddedValues.get(Enum1.B));
    }

    @Test
    public void testExternalClass() {
        assertFalse(getDs().getMapper().isMapped(HoldsUnannotated.class));
        assertFalse(getDs().getMapper().isMapped(ThirdPartyEmbedded.class));
        assertFalse(getDs().getMapper().isMapped(ThirdPartyEntity.class));

        assertThrows(MappingException.class, () -> getDs().getMapper().map(ThirdPartyEntity.class));
        assertThrows(MappingException.class, () -> getDs().getMapper().map(ThirdPartyEmbedded.class));

        getDs().getMapper().mapPackageFromClass(HoldsUnannotated.class);

        assertTrue(getDs().getMapper().isMapped(ThirdPartyEmbedded.class));
        assertTrue(getDs().getMapper().isMapped(ThirdPartyEntity.class));

        HoldsUnannotated holdsUnannotated = new HoldsUnannotated();
        holdsUnannotated.embedded = new ThirdPartyEmbedded();
        holdsUnannotated.embedded.number = 42L;
        holdsUnannotated.embedded.field = "Left";
        getDs().save(holdsUnannotated);

        assertEquals(getDs().find(HoldsUnannotated.class).first(), holdsUnannotated);

        withOptions(MapperOptions.DEFAULT, () -> {
            assertFalse(getDs().getMapper().map(ThirdPartyEntityProxy.class).isEmpty());
            assertFalse(getDs().getMapper().map(ThirdPartyEmbeddedProxy.class).isEmpty());

            EntityModel model = getDs().getMapper().getEntityModel(ThirdPartyEntity.class);
            assertEquals(model.getCollectionName(), "extEnt");
            assertEquals(model.getDiscriminator(), "ext");
            assertEquals(model.getDiscriminatorKey(), "_xt");
            Entity annotation = model.getAnnotation(Entity.class);
            assertEquals(annotation.concern(), "JOURNALED");
            assertEquals(annotation.cap().count(), 123);
            assertEquals(annotation.cap().value(), 456);

            ThirdPartyEntity entity = new ThirdPartyEntity();
            entity.field = "hi";
            entity.number = 42L;
            getDs().save(entity);

            assertEquals(getDs().find(ThirdPartyEntity.class).first(), entity);
        });
    }

    @Test
    public void testExternalClassUsingMethods() {
        withOptions(MapperOptions.builder()
                                 .propertyDiscovery(PropertyDiscovery.METHODS)
                                 .build(), () -> {
            assertFalse(getDs().getMapper().map(ThirdPartyEntityProxy.class).isEmpty());
            assertFalse(getDs().getMapper().map(ThirdPartyEmbeddedProxy.class).isEmpty());

            EntityModel model = getDs().getMapper().getEntityModel(ThirdPartyEntity.class);
            assertEquals(model.getCollectionName(), "extEnt");
            assertEquals(model.getDiscriminator(), "ext");
            assertEquals(model.getDiscriminatorKey(), "_xt");
            Entity annotation = model.getAnnotation(Entity.class);
            assertEquals(annotation.concern(), "JOURNALED");
            assertEquals(annotation.cap().count(), 123);
            assertEquals(annotation.cap().value(), 456);

            ThirdPartyEntity entity = new ThirdPartyEntity();
            entity.setField("hi");
            entity.setNumber(42L);
            getDs().save(entity);

            assertEquals(getDs().find(ThirdPartyEntity.class).first(), entity);
        });
    }

    @Test(dataProvider = "queryFactories")
    public void testFieldAsDiscriminator(QueryFactory queryFactory) {
        withOptions(MapperOptions.builder()
                                 .queryFactory(queryFactory)
                                 .enablePolymorphicQueries(true)
                                 .build(), () -> {
            getDs().getMapper().map(BlogImage.class, Png.class, Jpg.class);

            BlogImage png = new Png();
            png.content = "I'm a png";
            getDs().save(png);

            BlogImage jpg = new Jpg();
            jpg.content = "I'm a jpg";
            getDs().save(jpg);

            findFirst(getDs(), Png.class, png);
            findFirst(getDs(), Jpg.class, jpg);
            Query<BlogImage> query = getDs().find(BlogImage.class);
            assertEquals(query.count(), 2, query.toString());
            assertListEquals(query.iterator().toList(), List.of(jpg, png));
        });

    }

    @Test
    public void testFinalField() {
        getMapper().map(ContainsFinalField.class);
        final ObjectId savedKey = getDs().save(new ContainsFinalField("blah")).id;
        final ContainsFinalField loaded = getDs().find(ContainsFinalField.class)
                                                 .filter(eq("_id", savedKey))
                                                 .first();
        assertNotNull(loaded);
        assertNotNull(loaded.name);
        assertEquals(loaded.name, "blah");
    }

    @Test
    public void testFinalFieldNotPersisted() {
        withOptions(MapperOptions.builder(getMapper().getOptions())
                                 .ignoreFinals(true)
                                 .build(), () -> {
            getMapper().map(ContainsFinalField.class);
            final ObjectId savedKey = getDs().save(new ContainsFinalField("blah")).id;
            final Document loaded = getDs().getCollection(ContainsFinalField.class)
                                           .withDocumentClass(Document.class)
                                           .find(Filters.eq("_id", savedKey))
                                           .first();
            assertNotNull(loaded);
            assertNull(loaded.get("name"));
        });
    }

    @Test
    public void testFinalIdField() {
        getMapper().map(HasFinalFieldId.class);
        final long savedKey = getDs().save(new HasFinalFieldId(12)).id;
        final HasFinalFieldId loaded = getDs().find(HasFinalFieldId.class)
                                              .filter(eq("_id", savedKey))
                                              .first();
        assertNotNull(loaded);
        assertEquals(loaded.id, 12);
    }

    @Test
    @Ignore("need to add this feature")
    @SuppressWarnings("unchecked")
    public void testGenericKeyedMap() {
        final ContainsXKeyMap<Integer> map = new ContainsXKeyMap<>();
        map.values.put(1, "I'm 1");
        map.values.put(2, "I'm 2");

        getDs().save(map);

        final ContainsXKeyMap<Integer> mapLoaded = getDs().find(ContainsXKeyMap.class).filter(eq("_id", map.id)).first();

        assertNotNull(mapLoaded);


        Map<?, ?> values = mapLoaded.values;
        assertEquals(values.size(), 2);
        assertNotNull(values.get(1));
        assertNotNull(values.get(2));
    }

    @Test
    public void testIntKeySetStringMap() {
        final ContainsIntKeySetStringMap map = new ContainsIntKeySetStringMap();
        map.values.put(1, Collections.singleton("I'm 1"));
        map.values.put(2, Collections.singleton("I'm 2"));

        getDs().save(map);

        final ContainsIntKeySetStringMap mapLoaded = getDs().find(ContainsIntKeySetStringMap.class)
                                                            .filter(eq("_id", map.id))
                                                            .first();

        assertNotNull(mapLoaded);
        assertEquals(mapLoaded.values.size(), 2);
        assertNotNull(mapLoaded.values.get(1));
        assertNotNull(mapLoaded.values.get(2));
        assertEquals(mapLoaded.values.get(1).size(), 1);

        assertNotNull(getDs().find(ContainsIntKeyMap.class).filter(exists("values.2")));
        assertEquals(getDs().find(ContainsIntKeyMap.class).filter(exists("values.2").not()).count(), 0);
        assertNotNull(getDs().find(ContainsIntKeyMap.class).filter(exists("values.4").not()));
        assertEquals(getDs().find(ContainsIntKeyMap.class).filter(exists("values.4")).count(), 0);
    }

    @Test
    public void testIntKeyedMap() {
        final ContainsIntKeyMap map = new ContainsIntKeyMap();
        map.values.put(1, "I'm 1");
        map.values.put(2, "I'm 2");

        getDs().save(map);

        final ContainsIntKeyMap mapLoaded = getDs().find(ContainsIntKeyMap.class)
                                                   .filter(eq("_id", map.id))
                                                   .first();

        assertNotNull(mapLoaded);
        assertEquals(mapLoaded.values.size(), 2);
        assertNotNull(mapLoaded.values.get(1));
        assertNotNull(mapLoaded.values.get(2));

        assertNotNull(getDs().find(ContainsIntKeyMap.class)
                             .filter(exists("values.2")));
        assertEquals(getDs().find(ContainsIntKeyMap.class)
                            .filter(exists("values.2").not())
                            .count(), 0);
        assertNotNull(getDs().find(ContainsIntKeyMap.class)
                             .filter(exists("values.4").not()));
        assertEquals(getDs().find(ContainsIntKeyMap.class)
                            .filter(exists("values.4"))
                            .count(), 0);
    }

    @Test
    public void testIntLists() {
        ContainsIntegerList cil = new ContainsIntegerList();
        getDs().save(cil);
        ContainsIntegerList cilLoaded = getDs().find(ContainsIntegerList.class)
                                               .filter(eq("_id", cil.id))
                                               .first();
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(cilLoaded.intList.size(), cil.intList.size());


        cil = new ContainsIntegerList();
        cil.intList = null;
        getDs().save(cil);
        cilLoaded = getDs().find(ContainsIntegerList.class)
                           .filter(eq("_id", cil.id))
                           .first();
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(cilLoaded.intList.size(), 0);

        cil = new ContainsIntegerList();
        cil.intList.add(1);
        getDs().save(cil);
        cilLoaded = getDs().find(ContainsIntegerList.class)
                           .filter(eq("_id", cil.id))
                           .first();
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(cilLoaded.intList.size(), 1);
        assertEquals((int) cilLoaded.intList.get(0), 1);
    }

    @Test
    public void testLoadOnly() {
        getDs().save(new Normal("value"));
        Normal n = getDs().find(Normal.class).iterator(new FindOptions().limit(1))
                          .next();
        assertNotNull(n);
        assertNotNull(n.name);
        getDs().delete(n);
        getDs().save(new NormalWithLoadOnly());
        n = getDs().find(Normal.class).iterator(new FindOptions().limit(1))
                   .next();
        assertNotNull(n);
        assertNull(n.name);
        getDs().delete(n);
        getDs().save(new Normal("value21"));
        final NormalWithLoadOnly notSaved = getDs().find(NormalWithLoadOnly.class).iterator(new FindOptions().limit(1))
                                                   .next();
        assertNotNull(notSaved);
        assertNotNull(notSaved.name);
        assertEquals(notSaved.name, "never");
    }

    @Test
    public void testLongArrayMapping() {
        getMapper().map(ContainsLongAndStringArray.class);
        getDs().save(new ContainsLongAndStringArray());
        ContainsLongAndStringArray loaded = getDs().find(ContainsLongAndStringArray.class).iterator(new FindOptions().limit(1))
                                                   .next();
        assertEquals((new ContainsLongAndStringArray()).longs, loaded.longs);
        assertEquals((new ContainsLongAndStringArray()).strings, loaded.strings);

        final ContainsLongAndStringArray array = new ContainsLongAndStringArray();
        array.strings = new String[]{"a", "B", "c"};
        array.longs = new Long[]{4L, 5L, 4L};
        getDs().save(array);
        loaded = getDs().find(ContainsLongAndStringArray.class)
                        .filter(eq("_id", array.id))
                        .first();
        assertEquals(loaded.longs, array.longs);
        assertEquals(loaded.strings, array.strings);

        assertNotNull(loaded.id);
    }

    @Test
    public void testMapAsId() {
        getMapper().map(MapAsId.class);

        final MapAsId mai = new MapAsId();
        mai.id.put("test", "string");
        assertNotNull(getDs().save(mai));
        assertNotNull(getDs().find(MapAsId.class)
                             .filter(eq("_id", new Document("test", "string")))
                             .first());
    }

    @Test
    public void testMapLike() {
        final ContainsMapLike ml = new ContainsMapLike();
        ml.m.put("first", "test");
        getDs().save(ml);
        final ContainsMapLike mlLoaded = getDs().find(ContainsMapLike.class).iterator(new FindOptions().limit(1))
                                                .next();
        assertNotNull(mlLoaded);
        assertNotNull(mlLoaded.m);
        assertTrue(mlLoaded.m.containsKey("first"));
    }

    @Test
    public void testMapWithEmbeddedInterface() {
        final ContainsMapWithEmbeddedInterface aMap = new ContainsMapWithEmbeddedInterface();
        final Foo f1 = new Foo1();
        final Foo f2 = new Foo2();

        aMap.embeddedValues.put("first", f1);
        aMap.embeddedValues.put("second", f2);
        getDs().save(aMap);

        final ContainsMapWithEmbeddedInterface mapLoaded = getDs().find(ContainsMapWithEmbeddedInterface.class)
                                                                  .iterator(new FindOptions().limit(1))
                                                                  .next();

        assertNotNull(mapLoaded);
        assertEquals(mapLoaded.embeddedValues.size(), 2);
        assertTrue(mapLoaded.embeddedValues.get("first") instanceof Foo1);
        assertTrue(mapLoaded.embeddedValues.get("second") instanceof Foo2);

    }

    @Test

    public void testMethodMapping() {
        Datastore datastore = createDatastore(getMongoClient(), TEST_DB_NAME,
            MapperOptions.builder()
                         .propertyDiscovery(
                             PropertyDiscovery.METHODS)
                         .build());

        EntityModel model = datastore.getMapper().map(MethodMappedUser.class).get(0);
        assertTrue(model.getProperties().size() > 0);
        assertNotNull(model.getVersionProperty(), model.getProperties().toString());
        assertNotNull(model.getProperty("dateJoined"));
        assertNotNull(model.getProperty("joined"));
        assertNotNull(model.getProperty("friend_reference"));
        assertNotNull(model.getProperty("morphia_reference"));
    }

    @Test
    public void testObjectIdKeyedMap() {
        getMapper().map(ContainsObjectIdKeyMap.class);
        final ContainsObjectIdKeyMap map = new ContainsObjectIdKeyMap();
        final ObjectId o1 = new ObjectId("111111111111111111111111");
        final ObjectId o2 = new ObjectId("222222222222222222222222");
        map.values.put(o1, "I'm 1s");
        map.values.put(o2, "I'm 2s");

        getDs().save(map);

        final ContainsObjectIdKeyMap mapLoaded = getDs().find(ContainsObjectIdKeyMap.class).filter(eq("_id", map.id)).first();

        assertNotNull(mapLoaded);
        assertEquals(mapLoaded.values.size(), 2);
        assertNotNull(mapLoaded.values.get(o1));
        assertNotNull(mapLoaded.values.get(o2));

        assertNotNull(getDs().find(ContainsIntKeyMap.class).filter(exists("values.111111111111111111111111")));
        assertEquals(getDs().find(ContainsIntKeyMap.class).filter(exists("values.111111111111111111111111").not()).count(), 0);
        assertNotNull(getDs().find(ContainsIntKeyMap.class).filter(exists("values.4").not()));
        assertEquals(getDs().find(ContainsIntKeyMap.class).filter(exists("values.4")).count(), 0);
    }

    @Test
    public void testPrimMap() {
        final ContainsPrimitiveMap primMap = new ContainsPrimitiveMap();
        primMap.embeddedValues.put("first", 1L);
        primMap.embeddedValues.put("second", 2L);
        primMap.values.put("first", 1L);
        primMap.values.put("second", 2L);
        getDs().save(primMap);

        final ContainsPrimitiveMap primMapLoaded = getDs().find(ContainsPrimitiveMap.class)
                                                          .filter(eq("_id", primMap.id))
                                                          .first();

        assertNotNull(primMapLoaded);
        assertEquals(primMapLoaded.embeddedValues.size(), 2);
        assertEquals(primMapLoaded.values.size(), 2);
    }

    @Test
    public void testPrimMapWithNullValue() {
        final ContainsPrimitiveMap primMap = new ContainsPrimitiveMap();
        primMap.embeddedValues.put("first", null);
        primMap.embeddedValues.put("second", 2L);
        primMap.values.put("first", null);
        primMap.values.put("second", 2L);
        getDs().save(primMap);

        final ContainsPrimitiveMap primMapLoaded = getDs().find(ContainsPrimitiveMap.class)
                                                          .filter(eq("_id", primMap.id))
                                                          .first();

        assertNotNull(primMapLoaded);
        assertEquals(primMapLoaded.embeddedValues.size(), 2);
        assertEquals(primMapLoaded.values.size(), 2);
    }

    @Test
    //    @Tag("references")
    @Ignore("entity caching needs to be implemented")
    public void testRecursiveReference() {
/*
        getMapper().map(RecursiveParent.class, RecursiveChild.class);

        final RecursiveParent parent = getDs().save(new RecursiveParent());
        final RecursiveChild child = getDs().save(new RecursiveChild());

        final RecursiveParent parentLoaded = getDs().find(RecursiveParent.class)
                                                    .filter(eq("_id", parent.getId()))
                                                    .first();
        final RecursiveChild childLoaded = getDs().find(RecursiveChild.class)
                                                  .filter(eq("_id", child.getId()))
                                                  .first();

        parentLoaded.setChild(childLoaded);
        childLoaded.setParent(parentLoaded);

        getDs().save(parentLoaded);
        getDs().save(childLoaded);

        final RecursiveParent finalParentLoaded = getDs().find(RecursiveParent.class)
                                                         .filter(eq("_id", parent.getId()))
                                                         .first();
        final RecursiveChild finalChildLoaded = getDs().find(RecursiveChild.class)
                                                       .filter(eq("_id", child.getId()))
                                                       .first();


        assertNotNull(finalParentLoaded.getChild());
        assertNotNull(finalChildLoaded.getParent());
*/
    }

    @Test
    public void testReferenceWithoutIdValue() {
        assertThrows(ReferenceException.class, () -> {
            getMapper().map(Book.class, Author.class);
            final Book book = new Book();
            book.setAuthor(new Author());
            getDs().save(book);
        });
    }

    @Test
    public void testSubPackagesMapping() {
        // when
        withOptions(MapperOptions.builder(getMapper().getOptions())
                                 .mapSubPackages(true)
                                 .build(), () -> {
            getMapper().mapPackageFromClass(Versioned.class);

            // then
            List<EntityModel> list = getMapper().getMappedEntities();
            assertEquals(list.size(), 4, list.toString());
            Collection<Class<?>> classes = list.stream().map(EntityModel::getType)
                                               .collect(Collectors.toList());
            assertTrue(classes.contains(AbstractVersionedBase.class));
            assertTrue(classes.contains(Versioned.class));
            assertTrue(classes.contains(VersionedToo.class));
            assertTrue(classes.contains(VersionedChildEntity.class));

        });
    }

    @Test
    public void transientFields() {
        getMapper().map(HasTransientFields.class);
        final HasTransientFields entity = new HasTransientFields();
        entity.javaTransientString = "should not be persisted";
        entity.morphiaTransientString = "should not be persisted";
        entity.javaTransientInt = -1;
        entity.morphiaTransientInt = -1;

        getDs().save(entity);
        Document document = getDocumentCollection(HasTransientFields.class).find().first();
        String string = toString(document);
        Assert.assertFalse(document.containsKey("morphiaTransientString"), string);
        Assert.assertFalse(document.containsKey("morphiaTransientInt"), string);
        Assert.assertFalse(document.containsKey("javaTransientString"), string);
        Assert.assertFalse(document.containsKey("javaTransientInt"), string);
    }

    protected void findFirst(Datastore datastore, Class<?> type, BlogImage expected) {
        Query<?> query = datastore.find(type);
        assertEquals(query.count(), 1, query.toString());
        assertEquals(query.first(), expected, query.toString());
    }

    private void validateField(List<PropertyModel> fields, String mapped, String java) {
        assertNotNull(fields.stream().filter(f -> f.getMappedName().equals(mapped)
                                                  && f.getName().equals(java)),
            mapped);
    }

    private void verify(NamingStrategy strategy, String embeddedValues, String intList) {
        withOptions(MapperOptions.builder()
                                 .propertyNaming(strategy)
                                 .build(), () -> {
            List<EntityModel> map = getMapper().map(ContainsMapWithEmbeddedInterface.class, ContainsIntegerList.class);

            List<PropertyModel> fields = map.get(0).getProperties();
            validateField(fields, "_id", "id");
            validateField(fields, embeddedValues, "embeddedValues");

            fields = map.get(1).getProperties();
            validateField(fields, "_id", "id");
            validateField(fields, intList, "intList");
        });
    }

    private enum Enum1 {
        A,
        B
    }

    @Entity
    private interface Foo {
    }

    @Entity
    private abstract static class BaseEntity {
        @Id
        private ObjectId id;

        public String getId() {
            return id.toString();
        }

        public void setId(String id) {
            this.id = new ObjectId(id);
        }
    }

    @Entity
    private static class ConstructorBased {
        @Id
        private final ObjectId id;
        private final String name;
        private final MorphiaReference<ContainsFinalField> reference;

        public ConstructorBased(@Name("id") ObjectId id,
                                @Name("name") String name,
                                @Name("reference") MorphiaReference<ContainsFinalField> reference) {
            this.id = id;
            this.name = name;
            this.reference = reference;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (reference != null ? reference.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConstructorBased)) {
                return false;
            }

            final ConstructorBased that = (ConstructorBased) o;

            if (id != null ? !id.equals(that.id) : that.id != null) {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null) {
                return false;
            }
            return reference != null ? reference.equals(that.reference) : that.reference == null;
        }
    }

    @Entity
    private static class Containers {
        @Id
        private ObjectId id;

        private List<Float> floatList;
        private List<Byte> byteList;
    }

    @Entity
    private static class ContainsByteArray {
        private final byte[] bytes = "Scott".getBytes();
        @Id
        private ObjectId id;
    }

    @Entity
    private static final class ContainsCollection {
        private final Collection<String> coll = new ArrayList<>();
        @Id
        private ObjectId id;

        private ContainsCollection() {
            coll.add("hi");
            coll.add("Scott");
        }
    }

    @Entity
    private static class ContainsEmbeddedArray {
        @Id
        private final ObjectId id = new ObjectId();
        private RenamedEmbedded[] res;
    }

    @Entity
    private static class ContainsEmbeddedEntity {
        @Id
        private final ObjectId id = new ObjectId();
        private ContainsIntegerList cil = new ContainsIntegerList();
    }

    @Entity
    private static class ContainsEnum1KeyMap {
        private final Map<Enum1, String> values = new HashMap<>();
        private final Map<Enum1, String> embeddedValues = new HashMap<>();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class ContainsFinalField {
        private final String name;
        @Id
        private ObjectId id;

        protected ContainsFinalField() {
            name = "foo";
        }

        ContainsFinalField(String name) {
            this.name = name;
        }
    }

    @Entity
    private static class ContainsIntKeyMap {
        private final Map<Integer, String> values = new HashMap<>();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class ContainsIntKeySetStringMap {
        private final Map<Integer, Set<String>> values = new HashMap<>();
        @Id
        private ObjectId id;
    }

    @Entity(value = "cil", useDiscriminator = false)
    private static class ContainsIntegerList {
        @Id
        private ObjectId id;
        private List<Integer> intList = new ArrayList<>();
    }

    @Entity(value = "cil", useDiscriminator = false)
    private static class ContainsIntegerListNew {
        @AlsoLoad("intList")
        private final List<Integer> integers = new ArrayList<>();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class ContainsLongAndStringArray {
        @Id
        private ObjectId id;
        private Long[] longs = {0L, 1L, 2L};
        private String[] strings = {"Scott", "Rocks"};
    }

    @Entity
    private static class ContainsMapWithEmbeddedInterface {
        private final Map<String, Foo> embeddedValues = new HashMap<>();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class ContainsObjectIdKeyMap {
        private final Map<ObjectId, String> values = new HashMap<>();
        @Id
        private ObjectId id;

        @Override
        public String toString() {
            return new StringJoiner(", ", ContainsObjectIdKeyMap.class.getSimpleName() + "[", "]")
                       .add("id=" + id)
                       .add("values=" + values)
                       .toString();
        }
    }

    @Entity
    private static class ContainsPrimitiveMap {
        private final Map<String, Long> embeddedValues = new HashMap<>();
        private final Map<String, Long> values = new HashMap<>();
        @Id
        private ObjectId id;
    }

    private static class Foo1 implements Foo {
        private String s;
    }

    private static class Foo2 implements Foo {
        private int i;
    }

    @Entity
    private static class HasFinalFieldId {
        @Id
        private final long id;
        private final String name = "some string";

        protected HasFinalFieldId() {
            id = -1;
        }

        HasFinalFieldId(long id) {
            this.id = id;
        }
    }

    @Entity
    private static class HasTransientFields {
        @Id
        private ObjectId id;

        private transient String javaTransientString;
        private transient int javaTransientInt;
        @Transient
        private String morphiaTransientString;
        @Transient
        private int morphiaTransientInt;

    }

    @Entity
    private static class MapAsId {
        @Id
        private final Map<String, String> id = new HashMap<String, String>();
    }

    @Entity("generic_arrays")
    private static class MyEntity {
        @Id
        private String id;
        private Integer[] integers;
        private Super3<Integer>[] super3s;
    }

    @Entity(value = "Normal", useDiscriminator = false)
    private static class Normal {
        @Id
        private final ObjectId id = new ObjectId();
        private String name;

        Normal(String name) {
            this.name = name;
        }

        protected Normal() {
        }
    }

    @Entity(value = "Normal", useDiscriminator = false)
    private static class NormalWithLoadOnly {
        @Id
        private final ObjectId id = new ObjectId();
        @LoadOnly
        private final String name = "never";
    }

    @Entity(useDiscriminator = false)
    private static class RenamedEmbedded {
        private String name;
    }

    private static class Super1<T extends Object> {
        private T field;
    }

    private static class Super2<T extends Serializable> extends Super1<T> {
    }

    private static class Super3<T extends Number> extends Super2<T> {
    }

    @Entity
    public static class TwoIds {
        @Id
        private String extraId;
        @Id
        private String broken;
    }

    private static class UnannotatedEntity {
        @Id
        private ObjectId id;
        private String field;
        private Long number;
    }
}
