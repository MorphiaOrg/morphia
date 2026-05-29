package dev.morphia.test.mapping;

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
import java.util.stream.Stream;

import com.mongodb.client.model.Filters;

import dev.morphia.Datastore;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.LoadOnly;
import dev.morphia.annotations.Name;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.mapping.NotMappableException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.query.Query;
import dev.morphia.query.QueryException;
import dev.morphia.test.TestBase;
import dev.morphia.test.mapping.shadowing.ShadowedChild;
import dev.morphia.test.mapping.shadowing.ShadowedGrandChild;
import dev.morphia.test.mapping.shadowing.ShadowedGrandParent;
import dev.morphia.test.models.Author;
import dev.morphia.test.models.BannedUser;
import dev.morphia.test.models.BlogImage;
import dev.morphia.test.models.Book;
import dev.morphia.test.models.CityPopulation;
import dev.morphia.test.models.Jpg;
import dev.morphia.test.models.MappedInterface;
import dev.morphia.test.models.Png;
import dev.morphia.test.models.State;
import dev.morphia.test.models.TestEntity;
import dev.morphia.test.models.User;
import dev.morphia.test.models.errors.ContainsDocument;
import dev.morphia.test.models.errors.ContainsMapLike;
import dev.morphia.test.models.errors.ContainsXKeyMap;
import dev.morphia.test.models.errors.nonstaticinner.OuterClass.NonStaticInnerClass;
import dev.morphia.test.models.errors.twoIds.TwoIds;
import dev.morphia.test.models.errors.unannotated.UnannotatedEntity;
import dev.morphia.test.models.errors.unannotated.external.ThirdPartyEmbedded;
import dev.morphia.test.models.external.HoldsUnannotated;
import dev.morphia.test.models.external.ThirdPartyEmbeddedProxy;
import dev.morphia.test.models.external.ThirdPartyEntity;
import dev.morphia.test.models.external.ThirdPartyEntityProxy;
import dev.morphia.test.models.methods.MethodMappedUser;
import dev.morphia.test.models.versioned.AbstractVersionedBase;
import dev.morphia.test.models.versioned.Versioned;
import dev.morphia.test.models.versioned.VersionedChildEntity;
import dev.morphia.test.models.versioned.subversioned.VersionedToo;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static dev.morphia.mapping.PropertyDiscovery.FIELDS;
import static dev.morphia.mapping.PropertyDiscovery.METHODS;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static java.util.List.of;
import static java.util.stream.Collectors.toList;

@SuppressWarnings({ "unchecked", "unused" })
public class TestMapping extends TestBase {
    static Stream<Arguments> discovery() {
        return Stream.of(
                Arguments.of(buildConfig()
                        .packages(of(ShadowedGrandParent.class.getPackageName()))
                        .propertyDiscovery(FIELDS)),
                Arguments.of(buildConfig()
                        .packages(of(ShadowedGrandParent.class.getPackageName()))
                        .propertyDiscovery(METHODS)));
    }

    @ParameterizedTest
    @MethodSource("discovery")
    public void testShadowing(MorphiaConfig config) {
        assumeTrue(config.propertyDiscovery() != FIELDS, "Shadowing only works with METHODS discovery");
        withTestConfig(config, List.of(ShadowedGrandParent.class, ShadowedChild.class, ShadowedGrandChild.class),
                () -> {
                    checkShadowing("shadowed", ShadowedGrandParent.class, ShadowedGrandParent.class);
                    checkShadowing("shadowed", ShadowedChild.class, ShadowedChild.class);
                    checkShadowing("shadowed", ShadowedGrandChild.class, ShadowedGrandChild.class);
                });

    }

    private void checkShadowing(String name, Class<?> classType, Class<?> propertyType) {
        try {
            Class<?> actual = getMapper().getEntityModel(classType).getProperty(name).getType();
            Assertions.assertEquals(propertyType, actual, "Expected the field '%s' to be type '%s' on '%s' but was '%s'".formatted(
                    name, propertyType.getSimpleName(), classType.getSimpleName(), actual.getSimpleName()));
        } catch (NullPointerException e) {
            throw e;
        }
    }

    @Test
    public void childMapping() {
        Assertions.assertEquals("users", getMapper().getEntityModel(User.class).collectionName());
        Assertions.assertEquals("banned", getMapper().getEntityModel(BannedUser.class).collectionName());
    }

    @Test
    public void collectionNaming() {
        withConfig(buildConfig()
                .packages(of(ContainsMapWithEmbeddedInterface.class.getPackageName()))
                .collectionNaming(NamingStrategy.lowerCase()), () -> {
                    Assertions.assertEquals("containsmapwithembeddedinterface",
                            getMapper().getEntityModel(ContainsMapWithEmbeddedInterface.class).collectionName());
                    Assertions.assertEquals("cil", getMapper().getEntityModel(ContainsIntegerList.class).collectionName());
                });
        withConfig(buildConfig()
                .packages(of(ContainsMapWithEmbeddedInterface.class.getPackageName()))
                .collectionNaming(NamingStrategy.kebabCase()), () -> {
                    Assertions.assertEquals("contains-map-with-embedded-interface",
                            getMapper().getEntityModel(ContainsMapWithEmbeddedInterface.class).collectionName());
                    Assertions.assertEquals("cil", getMapper().getEntityModel(ContainsIntegerList.class).collectionName());
                });
    }

    @Test
    public void constructors() {
        ContainsFinalField value = new ContainsFinalField();
        ConstructorBased instance = new ConstructorBased(new ObjectId(), "test instance", value);

        getDs().save(of(value, instance));

        ConstructorBased first = getDs().find(ConstructorBased.class).first();
        Assertions.assertNotNull(first);
        Assertions.assertEquals(first, instance);
    }

    @Test
    public final void multipleIds() {
        Assertions.assertThrows(MappingException.class, () -> {
            withConfig(buildConfig().packages(of(TwoIds.class.getPackageName())), () -> {
                getMapper().getEntityModel(TwoIds.class);
            });
        });
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
        withConfig(buildConfig()
                .packages(of(Versioned.class.getPackageName())), () -> {

                    // then
                    List<EntityModel> list = getMapper().getMappedEntities();
                    Collection<Class<?>> classes = list.stream().map(EntityModel::getType)
                            .collect(Collectors.toList());
                    Assertions.assertEquals(3, classes.size());
                    Assertions.assertTrue(classes.contains(AbstractVersionedBase.class));
                    Assertions.assertTrue(classes.contains(Versioned.class));
                    Assertions.assertTrue(classes.contains(VersionedChildEntity.class));
                });
    }

    @Test
    public void shouldSupportGenericArrays() {
        Assertions.assertNotNull(getMapper().getEntityModel(MyEntity.class));
    }

    @Test
    public void subtypes() {
        EntityModel model = getMapper().getEntityModel(MappedInterface.class);
        Assertions.assertEquals(1, model.getSubtypes().size(), "Should find 1 subtype: " + model);

        model = getMapper().getEntityModel(User.class);
        Assertions.assertEquals(1, model.getSubtypes().size(), "Should find 1 subtype: " + model);
    }

    @Test
    public void testAlsoLoad() {
        final ContainsIntegerList cil = new ContainsIntegerList();
        cil.intList.add(1);
        getDs().save(cil);
        final ContainsIntegerList cilLoaded = getDs().find(ContainsIntegerList.class)
                .filter(eq("_id", cil.id))
                .first();
        Assertions.assertNotNull(cilLoaded);
        Assertions.assertNotNull(cilLoaded.intList);
        Assertions.assertEquals(cil.intList.size(), cilLoaded.intList.size());
        Assertions.assertEquals(cil.intList.get(0), cilLoaded.intList.get(0));

        final ContainsIntegerListNew cilNew = getDs().find(ContainsIntegerListNew.class).filter(eq("_id", cil.id)).first();
        Assertions.assertNotNull(cilNew);
        Assertions.assertNotNull(cilNew.integers);
        Assertions.assertEquals(1, cilNew.integers.size());
        Assertions.assertEquals(1, (int) cil.intList.get(0));
    }

    @Test
    public void testBadMappings() {
        withConfig(buildConfig()
                .packages(of(UnannotatedEntity.class.getPackageName())), () -> {
                    Assertions.assertThrows(NotMappableException.class, () -> {
                        getMapper().getEntityModel(UnannotatedEntity.class);
                        Assertions.fail("Missing @Entity and @Embedded should have been caught");
                    });
                });
        withConfig(buildConfig()
                .packages(of(ThirdPartyEmbedded.class.getPackageName())), () -> {
                    Assertions.assertThrows(NotMappableException.class, () -> {
                        getMapper().getEntityModel(ThirdPartyEmbedded.class);
                        Assertions.fail("Missing @Entity and @Embedded should have been caught");
                    });
                });
        withConfig(buildConfig()
                .packages(of(NonStaticInnerClass.class.getPackageName())), () -> {
                    Assertions.assertThrows(MappingException.class, () -> {
                        getMapper().getEntityModel(NonStaticInnerClass.class);
                        Assertions.fail("Validation: Non-static inner class allowed");
                    });
                });
    }

    @Test
    public void testBasicMapping() {
        Mapper mapper = getDs().getMapper();

        final State state = new State();
        state.state = "NY";
        state.biggest = new CityPopulation("NYC", 8336817L);
        state.smallest = new CityPopulation("Red House", 38L);

        getDs().save(state);

        Query<State> query = getDs().find(State.class)
                .filter(eq("_id", state.id));
        State loaded = query.first();

        Assertions.assertEquals(state, loaded);

        Assertions.assertEquals(of("_id", "state", "biggestCity", "smallestCity"), mapper.getEntityModel(State.class)
                .getProperties().stream()
                .map(PropertyModel::getMappedName)
                .collect(toList()));
    }

    @Test
    public void testByteArrayMapping() {
        final ObjectId savedKey = getDs().save(new ContainsByteArray()).id;
        final ContainsByteArray loaded = getDs().find(ContainsByteArray.class)
                .filter(eq("_id", savedKey))
                .first();
        Assertions.assertEquals(new String((new ContainsByteArray()).bytes), new String(loaded.bytes));
        Assertions.assertNotNull(loaded.id);
    }

    @Test
    public void testArrayOfNulls() {
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
        final ObjectId savedKey = getDs().save(new ContainsCollection()).id;
        final ContainsCollection loaded = getDs().find(ContainsCollection.class)
                .filter(eq("_id", savedKey))
                .first();
        Assertions.assertEquals(loaded.coll, (new ContainsCollection()).coll);
        Assertions.assertNotNull(loaded.id);
    }

    @Test
    public void testEmbeddedArrayElementHasNoClassname() {
        final ContainsEmbeddedArray cea = new ContainsEmbeddedArray();
        cea.res = new RenamedEmbedded[] { new RenamedEmbedded() };

        final Document document = toDocument(cea);
        List<Document> res = (List<Document>) document.get("res");
        Assertions.assertFalse(res.get(0).containsKey(getMapper().getConfig().discriminatorKey()));
    }

    @Test
    public void testEmbeddedDocument() {
        withConfig(buildConfig(ContainsDocument.class), () -> {
            getDs().save(new ContainsDocument());
            Assertions.assertNotNull(getDs().find(ContainsDocument.class).iterator()
                    .next());
        });
    }

    @Test
    public void testEmbeddedEntity() {
        getDs().save(new ContainsEmbeddedEntity());
        final ContainsEmbeddedEntity ceeLoaded = getDs().find(ContainsEmbeddedEntity.class)
                .iterator()
                .next();
        Assertions.assertNotNull(ceeLoaded);
        Assertions.assertNotNull(ceeLoaded.id);
        Assertions.assertNotNull(ceeLoaded.cil);
        Assertions.assertNull(ceeLoaded.cil.id);

    }

    @Test
    public void testEmbeddedEntityDocumentHasNoClassname() {
        final ContainsEmbeddedEntity cee = new ContainsEmbeddedEntity();
        cee.cil = new ContainsIntegerList();
        cee.cil.intList = Collections.singletonList(1);
        final Document document = toDocument(cee);
        Assertions.assertFalse(((Document) document.get("cil")).containsKey(getMapper().getConfig().discriminatorKey()));
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

        Assertions.assertNotNull(mapLoaded);
        Assertions.assertEquals(2, mapLoaded.values.size());
        Assertions.assertNotNull(mapLoaded.values.get(Enum1.A));
        Assertions.assertNotNull(mapLoaded.values.get(Enum1.B));
        Assertions.assertEquals(2, mapLoaded.embeddedValues.size());
        Assertions.assertNotNull(mapLoaded.embeddedValues.get(Enum1.A));
        Assertions.assertNotNull(mapLoaded.embeddedValues.get(Enum1.B));
    }

    @Test
    public void testExternalClass() {
        Assertions.assertFalse(getDs().getMapper().isMapped(HoldsUnannotated.class));
        Assertions.assertFalse(getDs().getMapper().isMapped(ThirdPartyEmbedded.class));
        Assertions.assertFalse(getDs().getMapper().isMapped(ThirdPartyEntity.class));

        Assertions.assertThrows(MappingException.class, () -> withTestConfig(List.of(ThirdPartyEntity.class), () -> {
        }));
        Assertions.assertThrows(MappingException.class, () -> withTestConfig(List.of(ThirdPartyEmbedded.class), () -> {
        }));

        withConfig(buildConfig(HoldsUnannotated.class), () -> {
            Assertions.assertTrue(getDs().getMapper().isMapped(ThirdPartyEmbedded.class));
            Assertions.assertTrue(getDs().getMapper().isMapped(ThirdPartyEntity.class));

            HoldsUnannotated holdsUnannotated = new HoldsUnannotated();
            holdsUnannotated.embedded = new ThirdPartyEmbedded();
            holdsUnannotated.embedded.number = 42L;
            holdsUnannotated.embedded.field = "Left";
            getDs().save(holdsUnannotated);

            Assertions.assertEquals(holdsUnannotated, getDs().find(HoldsUnannotated.class).first());

        });

        withTestConfig(List.of(ThirdPartyEntityProxy.class, ThirdPartyEmbeddedProxy.class), () -> {
            EntityModel model = getDs().getMapper().getEntityModel(ThirdPartyEntity.class);
            Assertions.assertEquals("extEnt", model.collectionName());
            Assertions.assertEquals("ext", model.discriminator());
            Assertions.assertEquals("_xt", model.discriminatorKey());
            Entity annotation = model.getAnnotation(Entity.class);
            Assertions.assertEquals("JOURNALED", annotation.concern());
            Assertions.assertEquals(123, annotation.cap().count());
            Assertions.assertEquals(456, annotation.cap().value());

            ThirdPartyEntity entity = new ThirdPartyEntity();
            entity.field = "hi";
            entity.number = 42L;
            getDs().save(entity);

            Assertions.assertEquals(entity, getDs().find(ThirdPartyEntity.class).first());
        });
    }

    @Test
    public void testExternalClassUsingMethods() {
        withConfig(buildConfig(ThirdPartyEntityProxy.class)
                .propertyDiscovery(METHODS), () -> {
                    List<EntityModel> mappedEntities = getMapper().getMappedEntities();
                    Assertions.assertFalse(mappedEntities.stream()
                            .map(EntityModel::getType)
                            .anyMatch(type -> type.equals(ThirdPartyEntityProxy.class) || type.equals(ThirdPartyEmbeddedProxy.class)));

                    EntityModel model = mappedEntities.stream()
                            .filter(m -> m.getType().equals(ThirdPartyEntity.class))
                            .findFirst()
                            .orElseThrow(
                                    () -> new MappingException(ThirdPartyEntity.class.getName() + " was not found"));
                    Assertions.assertEquals("extEnt", model.collectionName());
                    Assertions.assertEquals("ext", model.discriminator());
                    Assertions.assertEquals("_xt", model.discriminatorKey());
                    Entity annotation = model.getAnnotation(Entity.class);
                    Assertions.assertEquals("JOURNALED", annotation.concern());
                    Assertions.assertEquals(123, annotation.cap().count());
                    Assertions.assertEquals(456, annotation.cap().value());

                    ThirdPartyEntity entity = new ThirdPartyEntity();
                    entity.setField("hi");
                    entity.setNumber(42L);
                    getDs().save(entity);

                    Assertions.assertEquals(entity, getDs().find(ThirdPartyEntity.class).first());
                });
    }

    public void testFieldAsDiscriminator() {
        withConfig(buildConfig()
                .enablePolymorphicQueries(true), () -> {
                    BlogImage png = new Png();
                    png.content = "I'm a png";
                    getDs().save(png);

                    BlogImage jpg = new Jpg();
                    jpg.content = "I'm a jpg";
                    getDs().save(jpg);

                    findFirst(getDs(), Png.class, png);
                    findFirst(getDs(), Jpg.class, jpg);
                    Query<BlogImage> query = getDs().find(BlogImage.class);
                    Assertions.assertEquals(2, query.count(), query.toString());
                    assertListEquals(query.iterator().toList(), of(jpg, png));
                });

    }

    @Test
    public void testFinalField() {
        final ObjectId savedKey = getDs().save(new ContainsFinalField("blah")).id;
        final ContainsFinalField loaded = getDs().find(ContainsFinalField.class)
                .filter(eq("_id", savedKey))
                .first();
        Assertions.assertNotNull(loaded);
        Assertions.assertNotNull(loaded.name);
        Assertions.assertEquals("blah", loaded.name);
    }

    @Test
    public void testFinalFieldNotPersisted() {
        withConfig(buildConfig(ContainsFinalField.class)
                .ignoreFinals(true), () -> {
                    final ObjectId savedKey = getDs().save(new ContainsFinalField("blah")).id;
                    final Document loaded = getDs().getCollection(ContainsFinalField.class)
                            .withDocumentClass(Document.class)
                            .find(Filters.eq("_id", savedKey))
                            .first();
                    Assertions.assertNotNull(loaded);
                    Assertions.assertNull(loaded.get("name"));
                });
    }

    @Test
    public void testFinalIdField() {
        final long savedKey = getDs().save(new HasFinalFieldId(12)).id;
        final HasFinalFieldId loaded = getDs().find(HasFinalFieldId.class)
                .filter(eq("_id", savedKey))
                .first();
        Assertions.assertNotNull(loaded);
        Assertions.assertEquals(12, loaded.id);
    }

    @Test
    @Disabled("need to add this feature")
    @SuppressWarnings("unchecked")
    public void testGenericKeyedMap() {
        final ContainsXKeyMap<Integer> map = new ContainsXKeyMap<>();
        map.values.put(1, "I'm 1");
        map.values.put(2, "I'm 2");

        getDs().save(map);

        final ContainsXKeyMap<Integer> mapLoaded = getDs().find(ContainsXKeyMap.class).filter(eq("_id", map.id)).first();

        Assertions.assertNotNull(mapLoaded);

        Map<?, ?> values = mapLoaded.values;
        Assertions.assertEquals(2, values.size());
        Assertions.assertNotNull(values.get(1));
        Assertions.assertNotNull(values.get(2));
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

        Assertions.assertNotNull(mapLoaded);
        Assertions.assertEquals(2, mapLoaded.values.size());
        Assertions.assertNotNull(mapLoaded.values.get(1));
        Assertions.assertNotNull(mapLoaded.values.get(2));
        Assertions.assertEquals(1, mapLoaded.values.get(1).size());

        Assertions.assertNotNull(getDs().find(ContainsIntKeyMap.class).filter(exists("values.2")));
        Assertions.assertEquals(0, getDs().find(ContainsIntKeyMap.class).filter(exists("values.2").not()).count());
        Assertions.assertNotNull(getDs().find(ContainsIntKeyMap.class).filter(exists("values.4").not()));
        Assertions.assertEquals(0, getDs().find(ContainsIntKeyMap.class).filter(exists("values.4")).count());
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

        Assertions.assertNotNull(mapLoaded);
        Assertions.assertEquals(2, mapLoaded.values.size());
        Assertions.assertNotNull(mapLoaded.values.get(1));
        Assertions.assertNotNull(mapLoaded.values.get(2));

        Assertions.assertNotNull(getDs().find(ContainsIntKeyMap.class)
                .filter(exists("values.2")));
        Assertions.assertEquals(0, getDs().find(ContainsIntKeyMap.class)
                .filter(exists("values.2").not())
                .count());
        Assertions.assertNotNull(getDs().find(ContainsIntKeyMap.class)
                .filter(exists("values.4").not()));
        Assertions.assertEquals(0, getDs().find(ContainsIntKeyMap.class)
                .filter(exists("values.4"))
                .count());
    }

    @Test
    public void testIntLists() {
        ContainsIntegerList cil = new ContainsIntegerList();
        getDs().save(cil);
        ContainsIntegerList cilLoaded = getDs().find(ContainsIntegerList.class)
                .filter(eq("_id", cil.id))
                .first();
        Assertions.assertNotNull(cilLoaded);
        Assertions.assertNotNull(cilLoaded.intList);
        Assertions.assertEquals(cil.intList.size(), cilLoaded.intList.size());

        cil = new ContainsIntegerList();
        cil.intList = null;
        getDs().save(cil);
        cilLoaded = getDs().find(ContainsIntegerList.class)
                .filter(eq("_id", cil.id))
                .first();
        Assertions.assertNotNull(cilLoaded);
        Assertions.assertNotNull(cilLoaded.intList);
        Assertions.assertEquals(0, cilLoaded.intList.size());

        cil = new ContainsIntegerList();
        cil.intList.add(1);
        getDs().save(cil);
        cilLoaded = getDs().find(ContainsIntegerList.class)
                .filter(eq("_id", cil.id))
                .first();
        Assertions.assertNotNull(cilLoaded);
        Assertions.assertNotNull(cilLoaded.intList);
        Assertions.assertEquals(1, cilLoaded.intList.size());
        Assertions.assertEquals(1, (int) cilLoaded.intList.get(0));
    }

    @Test
    public void testLoadOnly() {
        getDs().save(new Normal("value"));
        Normal n = getDs().find(Normal.class).iterator()
                .next();
        Assertions.assertNotNull(n);
        Assertions.assertNotNull(n.name);
        getDs().delete(n);
        getDs().save(new NormalWithLoadOnly());
        n = getDs().find(Normal.class).iterator()
                .next();
        Assertions.assertNotNull(n);
        Assertions.assertNull(n.name);
        getDs().delete(n);
        getDs().save(new Normal("value21"));
        final NormalWithLoadOnly notSaved = getDs().find(NormalWithLoadOnly.class).iterator()
                .next();
        Assertions.assertNotNull(notSaved);
        Assertions.assertNotNull(notSaved.name);
        Assertions.assertEquals("never", notSaved.name);
    }

    @Test
    public void testLongArrayMapping() {
        getDs().save(new ContainsLongAndStringArray());
        ContainsLongAndStringArray loaded = getDs().find(ContainsLongAndStringArray.class).iterator()
                .next();
        Assertions.assertArrayEquals((new ContainsLongAndStringArray()).longs, loaded.longs);
        Assertions.assertArrayEquals((new ContainsLongAndStringArray()).strings, loaded.strings);

        final ContainsLongAndStringArray array = new ContainsLongAndStringArray();
        array.strings = new String[] { "a", "B", "c" };
        array.longs = new Long[] { 4L, 5L, 4L };
        getDs().save(array);
        loaded = getDs().find(ContainsLongAndStringArray.class)
                .filter(eq("_id", array.id))
                .first();
        Assertions.assertArrayEquals(array.longs, loaded.longs);
        Assertions.assertArrayEquals(array.strings, loaded.strings);

        Assertions.assertNotNull(loaded.id);
    }

    @Test
    public void testMapAsId() {
        final MapAsId mai = new MapAsId();
        mai.id.put("test", "string");
        Assertions.assertNotNull(getDs().save(mai));
        Assertions.assertNotNull(getDs().find(MapAsId.class)
                .filter(eq("_id", new Document("test", "string")))
                .first());
    }

    @Test
    public void testMapLike() {
        final ContainsMapLike ml = new ContainsMapLike();
        ml.m.put("first", "test");
        getDs().save(ml);
        final ContainsMapLike mlLoaded = getDs().find(ContainsMapLike.class).iterator()
                .next();
        Assertions.assertNotNull(mlLoaded);
        Assertions.assertNotNull(mlLoaded.m);
        Assertions.assertTrue(mlLoaded.m.containsKey("first"));
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
                .iterator()
                .next();

        Assertions.assertNotNull(mapLoaded);
        Assertions.assertEquals(2, mapLoaded.embeddedValues.size());
        Assertions.assertTrue(mapLoaded.embeddedValues.get("first") instanceof Foo1);
        Assertions.assertTrue(mapLoaded.embeddedValues.get("second") instanceof Foo2);

    }

    @Test
    public void testMethodMapping() {
        withConfig(buildConfig(MethodMappedUser.class).propertyDiscovery(METHODS), () -> {
            EntityModel model = getDs().getMapper().getEntityModel(MethodMappedUser.class);
            Assertions.assertTrue(model.getProperties().size() > 0);
            Assertions.assertNotNull(model.getVersionProperty(), model.getProperties().toString());
            Assertions.assertNotNull(model.getProperty("dateJoined"));
            Assertions.assertNotNull(model.getProperty("joined"));
            Assertions.assertNotNull(model.getProperty("friend_reference"));
            Assertions.assertNotNull(model.getProperty("morphia_reference"));
        });

    }

    @Test
    public void testObjectIdKeyedMap() {
        final ContainsObjectIdKeyMap map = new ContainsObjectIdKeyMap();
        final ObjectId o1 = new ObjectId("111111111111111111111111");
        final ObjectId o2 = new ObjectId("222222222222222222222222");
        map.values.put(o1, "I'm 1s");
        map.values.put(o2, "I'm 2s");

        getDs().save(map);

        final ContainsObjectIdKeyMap mapLoaded = getDs().find(ContainsObjectIdKeyMap.class).filter(eq("_id", map.id)).first();

        Assertions.assertNotNull(mapLoaded);
        Assertions.assertEquals(2, mapLoaded.values.size());
        Assertions.assertNotNull(mapLoaded.values.get(o1));
        Assertions.assertNotNull(mapLoaded.values.get(o2));

        Assertions.assertNotNull(getDs().find(ContainsIntKeyMap.class).filter(exists("values.111111111111111111111111")));
        Assertions.assertEquals(0, getDs().find(ContainsIntKeyMap.class).filter(exists("values.111111111111111111111111").not()).count());
        Assertions.assertNotNull(getDs().find(ContainsIntKeyMap.class).filter(exists("values.4").not()));
        Assertions.assertEquals(0, getDs().find(ContainsIntKeyMap.class).filter(exists("values.4")).count());
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

        Assertions.assertNotNull(primMapLoaded);
        Assertions.assertEquals(2, primMapLoaded.embeddedValues.size());
        Assertions.assertEquals(2, primMapLoaded.values.size());
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

        Assertions.assertNotNull(primMapLoaded);
        Assertions.assertEquals(2, primMapLoaded.embeddedValues.size());
        Assertions.assertEquals(2, primMapLoaded.values.size());
    }

    @Test
    @Tag("references")
    @Disabled("entity caching needs to be implemented")
    public void testRecursiveReference() {
        /*
         * getMapper().map(RecursiveParent.class, RecursiveChild.class);
         *
         * final RecursiveParent parent = getDs().save(new RecursiveParent());
         * final RecursiveChild child = getDs().save(new RecursiveChild());
         *
         * final RecursiveParent parentLoaded = getDs().find(RecursiveParent.class)
         * .filter(eq("_id", parent.getId()))
         * .first();
         * final RecursiveChild childLoaded = getDs().find(RecursiveChild.class)
         * .filter(eq("_id", child.getId()))
         * .first();
         *
         * parentLoaded.setChild(childLoaded);
         * childLoaded.setParent(parentLoaded);
         *
         * getDs().save(parentLoaded);
         * getDs().save(childLoaded);
         *
         * final RecursiveParent finalParentLoaded = getDs().find(RecursiveParent.class)
         * .filter(eq("_id", parent.getId()))
         * .first();
         * final RecursiveChild finalChildLoaded = getDs().find(RecursiveChild.class)
         * .filter(eq("_id", child.getId()))
         * .first();
         *
         *
         * assertNotNull(finalParentLoaded.getChild());
         * assertNotNull(finalChildLoaded.getParent());
         */
    }

    @Test
    public void testReferenceWithoutIdValue() {
        Assertions.assertThrows(QueryException.class, () -> {
            final Book book = new Book();
            book.author = new Author();
            getDs().save(book);
        });
    }

    @Test
    public void testSubPackagesMapping() {
        // when
        withConfig(buildConfig()
                .packages(of(Versioned.class.getPackageName() + ".*")), () -> {

                    // then
                    List<EntityModel> list = getMapper().getMappedEntities();
                    Assertions.assertEquals(4, list.size(), list.toString());
                    Collection<Class<?>> classes = list.stream().map(EntityModel::getType)
                            .collect(Collectors.toList());
                    Assertions.assertTrue(classes.contains(AbstractVersionedBase.class));
                    Assertions.assertTrue(classes.contains(Versioned.class));
                    Assertions.assertTrue(classes.contains(VersionedToo.class));
                    Assertions.assertTrue(classes.contains(VersionedChildEntity.class));

                });
    }

    @Test
    public void transientFields() {
        final HasTransientFields entity = new HasTransientFields();
        entity.javaTransientString = "should not be persisted";
        entity.morphiaTransientString = "should not be persisted";
        entity.javaTransientInt = -1;
        entity.morphiaTransientInt = -1;

        getDs().save(entity);
        Document document = getDocumentCollection(HasTransientFields.class).find().first();
        String string = toString(document);
        Assertions.assertFalse(document.containsKey("morphiaTransientString"), string);
        Assertions.assertFalse(document.containsKey("morphiaTransientInt"), string);
        Assertions.assertFalse(document.containsKey("javaTransientString"), string);
        Assertions.assertFalse(document.containsKey("javaTransientInt"), string);
    }

    protected void findFirst(Datastore datastore, Class<?> type, BlogImage expected) {
        Query<?> query = datastore.find(type);
        Assertions.assertEquals(1, query.count(), query.toString());
        Assertions.assertEquals(expected, query.first(), query.toString());
    }

    private void validateField(List<PropertyModel> fields, String mapped, String java) {
        Assertions.assertNotNull(fields.stream().filter(f -> f.getMappedName().equals(mapped)
                && f.getName().equals(java)),
                mapped);
    }

    private void verify(NamingStrategy strategy, String embeddedValues, String intList) {
        withConfig(buildConfig(ContainsMapWithEmbeddedInterface.class)
                .propertyNaming(strategy), () -> {

                    List<PropertyModel> fields = getMapper().getEntityModel(ContainsMapWithEmbeddedInterface.class).getProperties();
                    validateField(fields, "_id", "id");
                    validateField(fields, embeddedValues, "embeddedValues");

                    fields = getMapper().getEntityModel(ContainsIntegerList.class).getProperties();
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
        @Reference
        private final ContainsFinalField reference;

        public ConstructorBased(@Name("id") ObjectId id,
                @Name("name") String name,
                @Name("reference") ContainsFinalField reference) {
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

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ContainsFinalField)) {
                return false;
            }
            ContainsFinalField that = (ContainsFinalField) o;
            return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(name, that.name);
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
        private Long[] longs = { 0L, 1L, 2L };
        private String[] strings = { "Scott", "Rocks" };
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

}
